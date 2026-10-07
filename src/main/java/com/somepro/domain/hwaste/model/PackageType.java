package com.somepro.domain.hwaste.model;

/**
 * 包装方式（纯领域枚举）：DRUM 桶装 / BAG 袋装 / TANK 罐装 / BULK 散装。
 */
public enum PackageType {

    /** 桶装。 */
    DRUM,
    /** 袋装。 */
    BAG,
    /** 罐装。 */
    TANK,
    /** 散装。 */
    BULK;

    /** 是否合法的包装方式编码（大小写不敏感，入参先 trim）。 */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (PackageType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }
}
