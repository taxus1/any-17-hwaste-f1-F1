package com.somepro.infrastructure.persistence.hwaste.converter;

import com.somepro.domain.hwaste.model.CategoryStatus;
import com.somepro.domain.hwaste.model.WasteCategory;
import com.somepro.infrastructure.persistence.hwaste.po.WasteCategoryPO;

/**
 * WasteCategoryPO（表）↔ WasteCategory（领域）转换器（基础设施层）。
 */
public final class WasteCategoryPoConverter {

    private WasteCategoryPoConverter() {
    }

    public static WasteCategory toDomain(WasteCategoryPO po) {
        WasteCategory domain = new WasteCategory();
        domain.setId(po.getId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setName(po.getName());
        domain.setStatus(po.getStatus() == null ? null : CategoryStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
