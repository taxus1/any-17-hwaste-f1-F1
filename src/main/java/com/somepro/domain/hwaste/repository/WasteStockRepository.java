package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 入库批次仓储端口：领域层定义，基础设施层实现。
 */
public interface WasteStockRepository {

    /** 新入库：分配批次号（WB-年份-序号）并落库。 */
    Mono<WasteStock> inbound(WasteStock stock);

    /** 该单位该类别当前在库（IN_STOCK）批次重量合计；没有则为 0。 */
    Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode);

    /**
     * 联单转出：按入库先后 FIFO 消化在库批次，不足整批的拆分子批。
     * 在库合计不足时抛业务异常；返回实际转出重量。
     */
    Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg);

    /**
     * 拆分：把一个在库批次按重量拆成若干子批。
     * 子批重量合计必须正好等于父批重量；拆完父批作废（不再算在库），子批在库并指回父批。
     * 父批不在库 / 已被联单占用 / 合计对不上都抛业务异常，整笔不落；
     * 并发下同批只拆得动一次。
     */
    Mono<List<WasteStock>> split(Long batchId, List<BigDecimal> weights);

    /**
     * 合并：把同一单位、同一类别、同一包装的若干在库批次并成一票。
     * 合并批重量等于各批之和，原批全部作废（不再单独算在库）。
     * 任一批次不在库 / 已被联单占用 / 单位类别包装不一致都抛业务异常，整笔不落。
     */
    Mono<WasteStock> merge(List<Long> batchIds);

    /** 多条件分页查批次：单位 / 类别 / 包装 / 状态 / 入库日期区间均可选。 */
    Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                      String packageType, String status, LocalDate inDateFrom, LocalDate inDateTo);
}
