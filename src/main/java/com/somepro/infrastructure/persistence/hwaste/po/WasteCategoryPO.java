package com.somepro.infrastructure.persistence.hwaste.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_waste_category 表的持久化对象（PO，基础设施层）。
 */
@Getter
@Setter
@TableName("t_waste_category")
public class WasteCategoryPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("category_code")
    private String categoryCode;

    @TableField("name")
    private String name;

    @TableField("hazard_type")
    private String hazardType;

    @TableField("cross_province")
    private Integer crossProvince;

    @TableField("status")
    private String status;
}
