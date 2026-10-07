package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.domain.hwaste.model.WasteCategory;
import com.somepro.domain.hwaste.repository.WasteCategoryRepository;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.WasteCategoryPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.WasteCategoryPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 危废类别仓储适配器（基础设施层）。只读 lookup，供入库登记校验类别状态。
 */
@Repository
public class WasteCategoryRepositoryImpl extends BaseBlockingRepository implements WasteCategoryRepository {

    private final WasteCategoryMapper wasteCategoryMapper;

    public WasteCategoryRepositoryImpl(WasteCategoryMapper wasteCategoryMapper) {
        this.wasteCategoryMapper = wasteCategoryMapper;
    }

    @Override
    public Mono<WasteCategory> findByCode(String categoryCode) {
        return blocking(() -> {
            WasteCategoryPO po = wasteCategoryMapper.selectOne(Wrappers.<WasteCategoryPO>lambdaQuery()
                    .eq(WasteCategoryPO::getCategoryCode, categoryCode));
            return po == null ? null : WasteCategoryPoConverter.toDomain(po);
        });
    }
}
