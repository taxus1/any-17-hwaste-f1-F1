package com.somepro.infrastructure.persistence.hwaste.converter;

import com.somepro.domain.hwaste.model.SourceStatus;
import com.somepro.domain.hwaste.model.WasteSource;
import com.somepro.infrastructure.persistence.hwaste.po.WasteSourcePO;

/**
 * WasteSourcePO（表）↔ WasteSource（领域）转换器（基础设施层）。
 */
public final class WasteSourcePoConverter {

    private WasteSourcePoConverter() {
    }

    public static WasteSource toDomain(WasteSourcePO po) {
        WasteSource domain = new WasteSource();
        domain.setId(po.getId());
        domain.setSourceNo(po.getSourceNo());
        domain.setName(po.getName());
        domain.setStatus(po.getStatus() == null ? null : SourceStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
