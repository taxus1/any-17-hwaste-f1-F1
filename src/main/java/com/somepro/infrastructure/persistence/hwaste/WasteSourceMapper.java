package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.hwaste.po.WasteSourcePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * t_waste_source 的 MyBatis-Plus Mapper（基础设施层，阻塞 JDBC）。
 */
@Mapper
public interface WasteSourceMapper extends BaseMapper<WasteSourcePO> {
}
