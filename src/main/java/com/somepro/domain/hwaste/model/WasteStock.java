package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 危废入库批次（纯领域实体）。
 *
 * 账面在库重量 = 该单位该类别下 IN_STOCK 批次重量加总。
 * 盘点调账产生的对冲批次也落在这张表（盘盈正数、盘亏负数），
 * 与历史进出记录分行存放，不改写原有批次。
 *
 * 拆分与合并都不改写原批重量：原批置 VOID 退出在库账，
 * 子批 / 合并批另立一行接着算在库，子批用 parentBatchId 指回被拆的父批。
 */
@Getter
@Setter
public class WasteStock extends BaseEntity {

    private Long id;

    /** 入库批次号，全局唯一，形如 WB-2026-0001（由仓储层分配）。 */
    private String batchNo;

    private Long sourceId;

    private String categoryCode;

    /** 包装：DRUM 桶装 / BAG 袋装 / TANK 罐装 / BULK 散装。 */
    private String packageType;

    private BigDecimal weightKg;

    private LocalDateTime inAt;

    private StockStatus status;

    private Long manifestId;

    /** 拆分出的子批指向被拆的父批。 */
    private Long parentBatchId;

    /** 工厂方法：新入库（默认桶装，落在库状态，记入库时刻）。 */
    public static WasteStock inbound(Long sourceId, String categoryCode, BigDecimal weightKg, String packageType) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new BizException("入库重量必须大于 0");
        }
        WasteStock stock = new WasteStock();
        stock.setSourceId(sourceId);
        stock.setCategoryCode(categoryCode.trim());
        stock.setWeightKg(weightKg);
        stock.setPackageType((packageType == null || packageType.isBlank())
                ? PackageType.DRUM.name() : PackageType.of(packageType));
        stock.setStatus(StockStatus.IN_STOCK);
        stock.setInAt(LocalDateTime.now());
        return stock;
    }

    /**
     * 拆分：把本批次（必须在库且未被联单占住）按重量拆成若干子批。
     * 子批重量合计必须正好等于本批重量，一分不能多也不能少；
     * 子批继承本批的单位 / 类别 / 入库时刻，包装缺省继承、可按子批另指，
     * parentBatchId 指回本批。本批置 VOID 由仓储层落库。
     */
    public List<WasteStock> split(List<SplitItem> items) {
        ensureOperable("拆分");
        if (items == null || items.size() < 2) {
            throw new BizException("拆分至少需要两个子批");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SplitItem item : items) {
            if (item == null || item.weightKg() == null || item.weightKg().signum() <= 0) {
                throw new BizException("子批重量必须大于 0");
            }
            total = total.add(item.weightKg());
        }
        if (total.compareTo(this.weightKg) != 0) {
            throw new BizException("子批重量合计必须正好等于被拆批次重量，不能多也不能少");
        }
        List<WasteStock> children = new ArrayList<>();
        for (SplitItem item : items) {
            WasteStock child = new WasteStock();
            child.setSourceId(this.sourceId);
            child.setCategoryCode(this.categoryCode);
            child.setPackageType((item.packageType() == null || item.packageType().isBlank())
                    ? this.packageType : PackageType.of(item.packageType()));
            child.setWeightKg(item.weightKg());
            child.setInAt(this.inAt);
            child.setStatus(StockStatus.IN_STOCK);
            child.setParentBatchId(this.id);
            children.add(child);
        }
        return children;
    }

    /**
     * 合并：把同一家单位、同一个类别、同一种包装的若干在库批次并成一票。
     * 合并后的重量等于被并各批之和；被并批次置 VOID 由仓储层落库。
     */
    public static WasteStock mergeOf(List<WasteStock> batches) {
        if (batches == null || batches.size() < 2) {
            throw new BizException("合并至少需要两个在库批次");
        }
        WasteStock first = batches.get(0);
        BigDecimal total = BigDecimal.ZERO;
        for (WasteStock batch : batches) {
            batch.ensureOperable("合并");
            if (!Objects.equals(batch.getSourceId(), first.getSourceId())
                    || !Objects.equals(batch.getCategoryCode(), first.getCategoryCode())
                    || !Objects.equals(batch.getPackageType(), first.getPackageType())) {
                throw new BizException("仅同一单位、同一类别、同一包装的批次可合并");
            }
            total = total.add(batch.getWeightKg());
        }
        WasteStock merged = new WasteStock();
        merged.setSourceId(first.getSourceId());
        merged.setCategoryCode(first.getCategoryCode());
        merged.setPackageType(first.getPackageType());
        merged.setWeightKg(total);
        merged.setInAt(LocalDateTime.now());
        merged.setStatus(StockStatus.IN_STOCK);
        return merged;
    }

    /** 拆并前置校验：只有在库且未被联单占住的批次才能拆 / 并。 */
    public void ensureOperable(String action) {
        if (this.status != StockStatus.IN_STOCK) {
            throw new BizException("批次当前状态为 "
                    + (this.status == null ? "未知" : this.status.name()) + "，不允许" + action);
        }
        if (this.manifestId != null) {
            throw new BizException("批次已被联单占用，不允许" + action);
        }
    }
}
