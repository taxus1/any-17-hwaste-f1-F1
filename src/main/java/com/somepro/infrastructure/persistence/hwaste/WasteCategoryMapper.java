package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.hwaste.po.WasteCategoryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * t_waste_category 的 MyBatis-Plus Mapper（基础设施层，阻塞 JDBC）。
 */
@Mapper
public interface WasteCategoryMapper extends BaseMapper<WasteCategoryPO> {
}
