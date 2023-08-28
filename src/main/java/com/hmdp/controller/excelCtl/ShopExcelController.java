package com.hmdp.controller.excelCtl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopExcel;
import com.hmdp.service.IShopExcelService;
import com.hmdp.service.IShopService;
import com.hmdp.utils.excel.ExcelUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/exportExcel")
public class ShopExcelController {

    @Resource
    private IShopExcelService shopExcelService;

    @GetMapping("/shopList")
    public void export(HttpServletResponse response){
        List<ShopExcel> list = shopExcelService.getShopList();

        // 导出数据
       // ExcelUtils.exportTemplate(response, "用户表", ShopExcel.class);
        ExcelUtils.export(response,"用户表", list,ShopExcel.class);
    }
}
