package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.WasteSource;
import reactor.core.publisher.Mono;

/**
 * 产废单位仓储端口：领域层定义，基础设施层实现。入库登记据此校验单位状态。
 */
public interface WasteSourceRepository {

    /** 按 id 查产废单位；不存在返回空。 */
    Mono<WasteSource> findById(Long id);
}
