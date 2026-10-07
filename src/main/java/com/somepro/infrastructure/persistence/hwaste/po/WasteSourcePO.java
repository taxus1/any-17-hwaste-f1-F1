package com.somepro.infrastructure.persistence.hwaste.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_waste_source 表的持久化对象（PO，基础设施层）。
 */
@Getter
@Setter
@TableName("t_waste_source")
public class WasteSourcePO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("source_no")
    private String sourceNo;

    @TableField("name")
    private String name;

    @TableField("credit_code")
    private String creditCode;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("address")
    private String address;

    @TableField("contact")
    private String contact;

    @TableField("phone")
    private String phone;

    @TableField("status")
    private String status;
}
