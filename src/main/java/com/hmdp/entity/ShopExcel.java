package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hmdp.utils.excel.ExcelExport;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")

public class ShopExcel {

    @ExcelExport(value = "id",sort = 1)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 商铺名称
     */
    @ExcelExport(value ="名称",sort = 2)
    private String name;

    /**
     * 商圈，例如陆家嘴
     */
    @ExcelExport(value ="商圈",sort = 3)
    private String area;

    /**
     * 地址
     */
    @ExcelExport(value ="地址",sort = 4)
    private String address;

    /**
     * 均价，取整数
     */
    @ExcelExport(value ="均价",sort = 5)
    private Long avgPrice;

    /**
     * 销量
     */
    @ExcelExport(value ="销量",sort = 6)
    private Integer sold;

}
