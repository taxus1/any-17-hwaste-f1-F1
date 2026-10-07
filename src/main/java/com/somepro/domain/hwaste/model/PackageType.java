package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;

import java.util.Locale;

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

    /**
     * 归一化并校验包装方式：去空白、转大写；不在四种之内的挡回。
     * 返回归一化后的编码（领域对象里 packageType 仍以 String 存放）。
     */
    public static String of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("包装方式不能为空");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return PackageType.valueOf(normalized).name();
        } catch (IllegalArgumentException e) {
            throw new BizException("包装方式不合法：" + raw + "（仅支持 DRUM/BAG/TANK/BULK）");
        }
    }
}
