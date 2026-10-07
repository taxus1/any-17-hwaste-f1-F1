package com.somepro.domain.hwaste.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 危废类别名录（纯领域对象）。入库登记只关心它是否启用，
 * 因此这里只承载入库校验用到的字段。
 */
@Getter
@Setter
public class WasteCategory {

    private Long id;

    /** 危废类别代码，形如 HW08。 */
    private String categoryCode;

    private String name;

    private CategoryStatus status;

    /** 只有启用（ENABLED）的类别才允许登记入库。 */
    public boolean isEnabled() {
        return this.status == CategoryStatus.ENABLED;
    }
}
