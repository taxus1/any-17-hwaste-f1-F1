package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.WasteCategory;
import reactor.core.publisher.Mono;

/**
 * 危废类别仓储端口：领域层定义，基础设施层实现。入库登记据此校验类别状态。
 */
public interface WasteCategoryRepository {

    /** 按类别代码查危废类别；不存在返回空。 */
    Mono<WasteCategory> findByCode(String categoryCode);
}
