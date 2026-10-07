package com.somepro.domain.hwaste.model;

import java.math.BigDecimal;

/**
 * 拆分子批意图（领域值对象，不可变 record）：一份子批的重量 + 可选包装方式。
 * packageType 为 null 表示继承被拆父批的包装。
 */
public record SplitItem(BigDecimal weightKg, String packageType) {
}
