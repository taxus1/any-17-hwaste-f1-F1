package com.somepro.infrastructure.persistence.hwaste;

import com.somepro.domain.hwaste.model.WasteSource;
import com.somepro.domain.hwaste.repository.WasteSourceRepository;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.WasteSourcePoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.WasteSourcePO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 产废单位仓储适配器（基础设施层）。
 */
@Repository
public class WasteSourceRepositoryImpl extends BaseBlockingRepository implements WasteSourceRepository {

    private final WasteSourceMapper wasteSourceMapper;

    public WasteSourceRepositoryImpl(WasteSourceMapper wasteSourceMapper) {
        this.wasteSourceMapper = wasteSourceMapper;
    }

    @Override
    public Mono<WasteSource> findById(Long id) {
        return blocking(() -> {
            WasteSourcePO po = wasteSourceMapper.selectById(id);
            return po == null ? null : WasteSourcePoConverter.toDomain(po);
        });
    }
}
