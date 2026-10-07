package com.somepro.domain.hwaste.model;

import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 产废单位（纯领域实体）。入库登记只关心它存不存在、状态是否正常。
 */
@Getter
@Setter
public class WasteSource extends BaseEntity {

    private Long id;

    /** 产废单位编号，形如 WS-2026-0001。 */
    private String sourceNo;

    private String name;

    private SourceStatus status;

    /** 是否可正常办理入库：仅 ACTIVE。 */
    public boolean isActive() {
        return status == SourceStatus.ACTIVE;
    }
}
