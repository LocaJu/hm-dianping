package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopExcel;

import java.util.List;

public interface IShopExcelService extends IService<ShopExcel> {
    List<ShopExcel> getShopList();
}
