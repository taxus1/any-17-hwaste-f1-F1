package com.somepro.domain.hwaste.model;

import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 危废类别名录（纯领域实体）。入库登记只关心它存不存在、是否启用。
 */
@Getter
@Setter
public class WasteCategory extends BaseEntity {

    private Long id;

    /** 危废类别代码，形如 HW08。 */
    private String categoryCode;

    private String name;

    private CategoryStatus status;

    /** 是否可办理入库：仅 ENABLED。 */
    public boolean isEnabled() {
        return status == CategoryStatus.ENABLED;
    }
}
