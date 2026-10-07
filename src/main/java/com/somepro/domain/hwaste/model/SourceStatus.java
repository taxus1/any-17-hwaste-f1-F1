package com.somepro.domain.hwaste.model;

/**
 * 产废单位状态（纯领域枚举）。只有 ACTIVE 正常的单位才允许登记入库。
 */
public enum SourceStatus {

    /** 正常。 */
    ACTIVE,
    /** 停用。 */
    SUSPENDED,
    /** 关闭。 */
    CLOSED
}
