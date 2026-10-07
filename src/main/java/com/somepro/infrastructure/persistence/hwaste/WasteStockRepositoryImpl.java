package com.somepro.infrastructure.persistence.hwaste;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.StockStatus;
import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.WasteStockPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.WasteStockPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 入库批次仓储适配器（基础设施层）。
 *
 * 入库 / 转出 / 拆分 / 合并共用一把 WB 锁：批次号「取号 + 落库」串行，
 * 库存行的状态流转也串行，避免并发把同一批库存消化（或拆并）两遍。
 * 状态流转另有一道数据库闸：UPDATE 带 status = IN_STOCK 条件，并发下只有一个事务能改写成功。
 */
@Repository
public class WasteStockRepositoryImpl extends BaseBlockingRepository implements WasteStockRepository {

    private final WasteStockMapper wasteStockMapper;
    private final BizNoService bizNoService;
    private final TransactionTemplate txTemplate;

    public WasteStockRepositoryImpl(WasteStockMapper wasteStockMapper, BizNoService bizNoService,
                                    PlatformTransactionManager transactionManager) {
        this.wasteStockMapper = wasteStockMapper;
        this.bizNoService = bizNoService;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<WasteStock> inbound(WasteStock stock) {
        return blocking(() -> bizNoService.inLock("WB", () -> {
            WasteStockPO po = WasteStockPoConverter.toPo(stock);
            po.setId(IdUtil.getSnowflakeNextId());
            po.setBatchNo(bizNoService.nextBatchNo());
            wasteStockMapper.insert(po);
            return WasteStockPoConverter.toDomain(po);
        }));
    }

    @Override
    public Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode) {
        return blocking(() -> wasteStockMapper.sumInStockWeight(sourceId, categoryCode));
    }

    @Override
    public Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg) {
        return blocking(() -> bizNoService.inLock("WB", () -> txTemplate.execute(tx -> {
            List<WasteStockPO> batches = wasteStockMapper.selectList(Wrappers.<WasteStockPO>lambdaQuery()
                    .eq(WasteStockPO::getSourceId, sourceId)
                    .eq(WasteStockPO::getCategoryCode, categoryCode)
                    .eq(WasteStockPO::getStatus, StockStatus.IN_STOCK.name())
                    .orderByAsc(WasteStockPO::getInAt)
                    .orderByAsc(WasteStockPO::getId));
            BigDecimal total = batches.stream()
                    .map(WasteStockPO::getWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(weightKg) < 0) {
                throw new BizException("在库重量不足，无法转出");
            }
            BigDecimal remaining = weightKg;
            for (WasteStockPO batch : batches) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal weight = batch.getWeightKg();
                if (weight.compareTo(remaining) <= 0) {
                    // 整批转出
                    remaining = remaining.subtract(weight);
                    WasteStockPO update = new WasteStockPO();
                    update.setId(batch.getId());
                    update.setStatus(StockStatus.TRANSFERRED.name());
                    wasteStockMapper.updateById(update);
                } else {
                    // 部分转出：父批作废，拆出「转出部分」与「留存部分」两个子批，父批原记录保留
                    insertChild(batch, remaining, StockStatus.TRANSFERRED);
                    insertChild(batch, weight.subtract(remaining), StockStatus.IN_STOCK);
                    WasteStockPO update = new WasteStockPO();
                    update.setId(batch.getId());
                    update.setStatus(StockStatus.VOID.name());
                    wasteStockMapper.updateById(update);
                    remaining = BigDecimal.ZERO;
                }
            }
            return weightKg;
        })));
    }

    @Override
    public Mono<List<WasteStock>> split(Long batchId, List<BigDecimal> weights) {
        return blocking(() -> bizNoService.inLock("WB", () -> txTemplate.execute(tx -> {
            WasteStockPO parent = wasteStockMapper.selectById(batchId);
            if (parent == null) {
                throw new BizException("批次不存在");
            }
            WasteStockPoConverter.toDomain(parent).assertOperatable("拆分");
            BigDecimal sum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(parent.getWeightKg()) != 0) {
                throw new BizException("拆分重量合计必须正好等于原批重量 " + parent.getWeightKg() + " 千克，一分不能多也不能少");
            }
            // 状态闸：只有在库批能被置作废；并发重复拆分时后到的更新 0 行，整笔回滚，不会拆出两份
            markVoidIfInStock(parent.getId(), "拆分");
            List<WasteStock> children = new ArrayList<>();
            for (BigDecimal weight : weights) {
                children.add(insertChild(parent, weight, StockStatus.IN_STOCK));
            }
            return children;
        })));
    }

    @Override
    public Mono<WasteStock> merge(List<Long> batchIds) {
        return blocking(() -> bizNoService.inLock("WB", () -> txTemplate.execute(tx -> {
            // 按 id 升序加载并置作废，固定顺序避免并发合并互相等锁
            List<Long> ids = batchIds.stream().distinct().sorted().collect(Collectors.toList());
            List<WasteStockPO> batches = new ArrayList<>();
            for (Long id : ids) {
                WasteStockPO po = wasteStockMapper.selectById(id);
                if (po == null) {
                    throw new BizException("批次不存在：id=" + id);
                }
                WasteStockPoConverter.toDomain(po).assertOperatable("合并");
                batches.add(po);
            }
            WasteStockPO first = batches.get(0);
            for (WasteStockPO batch : batches) {
                if (!first.getSourceId().equals(batch.getSourceId())
                        || !first.getCategoryCode().equals(batch.getCategoryCode())
                        || !first.getPackageType().equals(batch.getPackageType())) {
                    throw new BizException("只有同一单位、同一类别、同一包装的在库批次才能合并");
                }
            }
            BigDecimal total = batches.stream()
                    .map(WasteStockPO::getWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDateTime earliestInAt = batches.stream()
                    .map(WasteStockPO::getInAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
            // 状态闸：任一原批已不在库（被并过 / 拆过 / 转走）都会更新 0 行，整笔回滚
            for (WasteStockPO batch : batches) {
                markVoidIfInStock(batch.getId(), "合并");
            }
            // 合并批：重量为各批之和，入库时刻取各批最早（货还是那个时点进的库），重新算在库
            WasteStockPO merged = new WasteStockPO();
            merged.setId(IdUtil.getSnowflakeNextId());
            merged.setBatchNo(bizNoService.nextBatchNo());
            merged.setSourceId(first.getSourceId());
            merged.setCategoryCode(first.getCategoryCode());
            merged.setPackageType(first.getPackageType());
            merged.setWeightKg(total);
            merged.setInAt(earliestInAt);
            merged.setStatus(StockStatus.IN_STOCK.name());
            wasteStockMapper.insert(merged);
            return WasteStockPoConverter.toDomain(merged);
        })));
    }

    @Override
    public Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String packageType, String status,
                                             LocalDate inDateFrom, LocalDate inDateTo) {
        return this.<PageResult<WasteStock>>blocking(() -> {
            try {
                // 条件值为 null 时包装器也会先求值第三个参数，先归一化再拼条件，避免空指针
                String pkg = (packageType == null || packageType.isBlank()) ? null : packageType.trim().toUpperCase();
                LocalDateTime inFrom = inDateFrom == null ? null : inDateFrom.atStartOfDay();
                LocalDateTime inToExclusive = inDateTo == null ? null : inDateTo.plusDays(1).atStartOfDay();
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<WasteStockPO> wrapper = Wrappers.<WasteStockPO>lambdaQuery()
                        .eq(sourceId != null, WasteStockPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                WasteStockPO::getCategoryCode, categoryCode)
                        .eq(pkg != null, WasteStockPO::getPackageType, pkg)
                        .eq(status != null && !status.isBlank(), WasteStockPO::getStatus, status)
                        .ge(inFrom != null, WasteStockPO::getInAt, inFrom)
                        .lt(inToExclusive != null, WasteStockPO::getInAt, inToExclusive)
                        .orderByDesc(WasteStockPO::getId);
                List<WasteStockPO> rows = wasteStockMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<WasteStock> content = rows.stream()
                        .map(WasteStockPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    /** 拆分子批：继承父批的单位 / 类别 / 包装 / 入库时刻，parent_batch_id 指回父批。 */
    private WasteStock insertChild(WasteStockPO parent, BigDecimal weight, StockStatus status) {
        WasteStockPO child = new WasteStockPO();
        child.setId(IdUtil.getSnowflakeNextId());
        child.setBatchNo(bizNoService.nextBatchNo());
        child.setSourceId(parent.getSourceId());
        child.setCategoryCode(parent.getCategoryCode());
        child.setPackageType(parent.getPackageType());
        child.setWeightKg(weight);
        child.setInAt(parent.getInAt());
        child.setStatus(status.name());
        child.setParentBatchId(parent.getId());
        wasteStockMapper.insert(child);
        return WasteStockPoConverter.toDomain(child);
    }

    /** 状态闸：仅当批次仍在库时置作废；更新 0 行说明已被别处动过，整笔回滚。 */
    private void markVoidIfInStock(Long batchId, String action) {
        WasteStockPO statusUpdate = new WasteStockPO();
        statusUpdate.setStatus(StockStatus.VOID.name());
        int rows = wasteStockMapper.update(statusUpdate, Wrappers.<WasteStockPO>lambdaUpdate()
                .eq(WasteStockPO::getId, batchId)
                .eq(WasteStockPO::getStatus, StockStatus.IN_STOCK.name()));
        if (rows == 0) {
            throw new BizException("批次状态已变化（可能刚被" + action + "或转出过），本次" + action + "未执行");
        }
    }
}
