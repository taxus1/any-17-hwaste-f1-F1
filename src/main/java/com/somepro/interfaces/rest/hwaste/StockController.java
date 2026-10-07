package com.somepro.interfaces.rest.hwaste;

import com.somepro.application.hwaste.StockAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.hwaste.converter.WasteStockVoConverter;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferResultVO;
import com.somepro.interfaces.rest.hwaste.vo.WasteStockVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 危废入库批次接口（用户接口层）：新入库、拆分、合并、联单转出、在库合计与批次分页。
 *
 * 入库看两头：产废单位 ACTIVE、危废类别 ENABLED 才让登。
 * 盘点冻结规则：该单位该类别有单子在盘点中（COUNTING / PENDING_APPROVAL / APPROVED）时，
 * 新入库与联单转出都先挡回；没在盘点中的组合照常放行。
 */
@RestController
@RequestMapping("/api/hwaste/stock")
public class StockController {

    private final StockAppService stockAppService;

    public StockController(StockAppService stockAppService) {
        this.stockAppService = stockAppService;
    }

    /** 新入库：生成 WB 批次，落在库状态。 */
    @PostMapping("/inbound")
    public Mono<Result<WasteStockVO>> inbound(@RequestParam(required = false) Long sourceId,
                                              @RequestParam(required = false) String categoryCode,
                                              @RequestParam(required = false) BigDecimal weightKg,
                                              @RequestParam(required = false) String packageType) {
        return stockAppService.inbound(sourceId, categoryCode, weightKg, packageType)
                .map(WasteStockVoConverter::toVo)
                .map(Result::ok);
    }

    /** 拆分：把一个在库批次按重量拆成几个子批，子批合计必须正好等于原批重量。 */
    @PostMapping("/split")
    public Mono<Result<List<WasteStockVO>>> split(@RequestParam(required = false) Long batchId,
                                                  @RequestParam(required = false) List<BigDecimal> weights) {
        return stockAppService.split(batchId, weights)
                .map(WasteStockVoConverter::toVoList)
                .map(Result::ok);
    }

    /** 合并：把同一单位、同一类别、同一包装的几个在库批次并成一票。 */
    @PostMapping("/merge")
    public Mono<Result<WasteStockVO>> merge(@RequestParam(required = false) List<Long> batchIds) {
        return stockAppService.merge(batchIds)
                .map(WasteStockVoConverter::toVo)
                .map(Result::ok);
    }

    /** 联单转出：按入库先后 FIFO 消化在库批次，不足整批的拆分子批。 */
    @PostMapping("/transfer-out")
    public Mono<Result<TransferResultVO>> transferOut(@RequestParam(required = false) Long sourceId,
                                                      @RequestParam(required = false) String categoryCode,
                                                      @RequestParam(required = false) BigDecimal weightKg) {
        return stockAppService.transferOut(sourceId, categoryCode, weightKg)
                .map(transferred -> new TransferResultVO(sourceId, categoryCode, transferred))
                .map(Result::ok);
    }

    /** 该单位该类别当前在库重量合计。 */
    @GetMapping("/sum")
    public Mono<Result<BigDecimal>> sumInStock(@RequestParam(required = false) Long sourceId,
                                               @RequestParam(required = false) String categoryCode) {
        return stockAppService.sumInStock(sourceId, categoryCode).map(Result::ok);
    }

    /** 批次分页查询：单位 / 类别 / 包装 / 状态 / 入库日期区间均可选，啥都不挑分页列全。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<WasteStockVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String packageType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inDateTo) {
        return stockAppService.page(pageNum, pageSize, sourceId, categoryCode, packageType, status,
                        inDateFrom, inDateTo)
                .map(WasteStockVoConverter::toPageVo)
                .map(Result::ok);
    }
}
