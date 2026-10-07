package com.somepro.interfaces.rest.hwaste.converter;

import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.WasteStockVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WasteStock（领域）→ 对外 VO 转换器（用户接口层）。
 */
public final class WasteStockVoConverter {

    private WasteStockVoConverter() {
    }

    public static WasteStockVO toVo(WasteStock domain) {
        return new WasteStockVO(
                domain.getId(),
                domain.getBatchNo(),
                domain.getSourceId(),
                domain.getCategoryCode(),
                domain.getPackageType(),
                domain.getWeightKg(),
                domain.getInAt(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getManifestId(),
                domain.getParentBatchId(),
                domain.getCreateTime());
    }

    public static List<WasteStockVO> toVoList(List<WasteStock> domains) {
        return domains.stream().map(WasteStockVoConverter::toVo).collect(Collectors.toList());
    }

    public static PageVO<WasteStockVO> toPageVo(PageResult<WasteStock> page) {
        List<WasteStockVO> content = page.content().stream()
                .map(WasteStockVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
