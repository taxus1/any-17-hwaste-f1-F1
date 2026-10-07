package com.somepro.application.hwaste;

import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.hwaste.repository.StockCheckRepository;
import com.somepro.domain.hwaste.repository.WasteCategoryRepository;
import com.somepro.domain.hwaste.repository.WasteSourceRepository;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 入库 / 拆分 / 合并 / 转出用例编排（应用层）。
 *
 * 入库看两头：产废单位 ACTIVE、危废类别 ENABLED 才让登，停用的一律挡回。
 * 盘点冻结规则：该单位该类别一旦有单子进到盘点中（COUNTING / PENDING_APPROVAL / APPROVED），
 * 新入库与联单转出都先停下来，等调完账或作废再放行；没在盘点中的组合照常放行。
 * 拆分与合并不改在库总账（子批合计 = 父批、合并批 = 各批之和），不受盘点冻结影响。
 */
@Service
public class StockAppService {

    private final WasteStockRepository wasteStockRepository;
    private final StockCheckRepository stockCheckRepository;
    private final WasteSourceRepository wasteSourceRepository;
    private final WasteCategoryRepository wasteCategoryRepository;

    public StockAppService(WasteStockRepository wasteStockRepository, StockCheckRepository stockCheckRepository,
                           WasteSourceRepository wasteSourceRepository,
                           WasteCategoryRepository wasteCategoryRepository) {
        this.wasteStockRepository = wasteStockRepository;
        this.stockCheckRepository = stockCheckRepository;
        this.wasteSourceRepository = wasteSourceRepository;
        this.wasteCategoryRepository = wasteCategoryRepository;
    }

    /** 新入库：单位 / 类别状态校验 + 盘点冻结校验，都过了才落批次。 */
    public Mono<WasteStock> inbound(Long sourceId, String categoryCode, BigDecimal weightKg, String packageType) {
        return Mono.defer(() -> {
            WasteStock stock = WasteStock.inbound(sourceId, categoryCode, weightKg, packageType);
            return wasteSourceRepository.findById(stock.getSourceId())
                    .switchIfEmpty(Mono.error(new BizException("产废单位不存在")))
                    .flatMap(source -> {
                        if (!source.isActive()) {
                            return Mono.error(new BizException("产废单位未处于正常状态（ACTIVE），不能登记入库"));
                        }
                        return wasteCategoryRepository.findByCode(stock.getCategoryCode())
                                .switchIfEmpty(Mono.error(new BizException("危废类别不存在")))
                                .flatMap(category -> {
                                    if (!category.isEnabled()) {
                                        return Mono.error(new BizException("危废类别已停用（DISABLED），不能登记入库"));
                                    }
                                    return rejectIfFrozen(stock.getSourceId(), stock.getCategoryCode(), "新入库")
                                            .then(wasteStockRepository.inbound(stock));
                                });
                    });
        });
    }

    /**
     * 拆分：把一个在库批次按重量拆成几个子批。
     * 子批重量合计必须正好等于原批重量；重复点 / 并发点同批只拆得动一次。
     */
    public Mono<List<WasteStock>> split(Long batchId, List<BigDecimal> weights) {
        return Mono.defer(() -> {
            if (batchId == null) {
                return Mono.error(new BizException("被拆批次不能为空"));
            }
            if (weights == null || weights.size() < 2) {
                return Mono.error(new BizException("拆分至少给出 2 个子批重量"));
            }
            if (weights.stream().anyMatch(w -> w == null || w.signum() <= 0)) {
                return Mono.error(new BizException("每个子批重量都必须大于 0"));
            }
            return wasteStockRepository.split(batchId, weights);
        });
    }

    /**
     * 合并：把同一单位、同一类别、同一包装的几个在库批次并成一票。
     * 合并批重量等于各批之和；原批不再单独算在库；重复点 / 并发点只并得动一次。
     */
    public Mono<WasteStock> merge(List<Long> batchIds) {
        return Mono.defer(() -> {
            if (batchIds == null || batchIds.stream().filter(Objects::nonNull).distinct().count() < 2) {
                return Mono.error(new BizException("合并至少需要 2 个不同的批次"));
            }
            return wasteStockRepository.merge(batchIds);
        });
    }

    /** 联单转出：盘点冻结中的组合先挡回；在库不足也挡回。 */
    public Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            if (weightKg == null || weightKg.signum() <= 0) {
                return Mono.error(new BizException("转出重量必须大于 0"));
            }
            return rejectIfFrozen(sourceId, categoryCode.trim(), "联单转出")
                    .then(wasteStockRepository.transferOut(sourceId, categoryCode.trim(), weightKg));
        });
    }

    /** 该单位该类别当前在库重量合计。 */
    public Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            return wasteStockRepository.sumInStock(sourceId, categoryCode.trim());
        });
    }

    public Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String packageType, String status,
                                             LocalDate inDateFrom, LocalDate inDateTo) {
        return wasteStockRepository.page(pageNum, pageSize, sourceId, categoryCode, packageType, status,
                inDateFrom, inDateTo);
    }

    /** 盘点冻结校验：该单位该类别有盘点中的单子就挡回。 */
    private Mono<Void> rejectIfFrozen(Long sourceId, String categoryCode, String action) {
        return stockCheckRepository.countFreezing(sourceId, categoryCode)
                .flatMap(frozen -> frozen != null && frozen > 0
                        ? Mono.error(new BizException("该单位该类别正在盘点中，" + action + "暂停，待盘点调账或作废后放行"))
                        : Mono.empty());
    }
}
