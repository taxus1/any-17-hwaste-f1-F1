package com.somepro.domain.hwaste.model;

/**
 * 危废类别状态（纯领域枚举）。只有 ENABLED 启用的类别才允许登记入库。
 */
public enum CategoryStatus {

    /** 启用。 */
    ENABLED,
    /** 停用。 */
    DISABLED
}
