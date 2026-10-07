package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.SplitItem;
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
     * 拆分：把一个在库批次按重量拆成若干子批，父批置 VOID 退出在库账。
     * 已拆过 / 并过的批次（VOID）幂等处理：直接返回现有子批，不再重复拆；
     * 已转出 / 已处置 / 被联单占住的批次抛业务异常。
     */
    Mono<List<WasteStock>> split(Long batchId, List<SplitItem> items);

    /**
     * 合并：把同单位、同类别、同包装的若干在库批次并成一票，被并批次置 VOID。
     * 已并过 / 拆过的批次（VOID）跳过；剩下的在库批次不足两个时幂等空操作，返回空。
     * 已转出 / 已处置 / 被联单占住的批次抛业务异常。
     */
    Mono<WasteStock> merge(List<Long> batchIds);

    /** 多条件分页查批次：单位 / 类别 / 包装 / 状态 / 入库日期区间均可选。 */
    Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                      String packageType, String status,
                                      LocalDate inDateFrom, LocalDate inDateTo);
}
