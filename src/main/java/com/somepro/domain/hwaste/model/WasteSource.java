package com.somepro.domain.hwaste.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 产废单位（纯领域对象）。入库登记只关心它是否处在正常状态，
 * 因此这里只承载入库校验用到的字段。
 */
@Getter
@Setter
public class WasteSource {

    private Long id;

    /** 产废单位编号，形如 WS-2026-0001。 */
    private String sourceNo;

    private String name;

    private SourceStatus status;

    /** 只有正常（ACTIVE）的单位才允许登记入库。 */
    public boolean isActive() {
        return this.status == SourceStatus.ACTIVE;
    }
}
