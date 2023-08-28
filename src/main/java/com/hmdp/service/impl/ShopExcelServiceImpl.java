package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopExcel;
import com.hmdp.mapper.ShopExcelMapper;
import com.hmdp.service.IShopExcelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ShopExcelServiceImpl extends ServiceImpl<ShopExcelMapper, ShopExcel> implements IShopExcelService {

    @Resource
    private ShopExcelMapper shopExcelMapper;

    @Override
    public List<ShopExcel> getShopList() {

        QueryWrapper<ShopExcel> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","name", "area", "address", "avg_price","sold");
        List<ShopExcel> shopExcels = shopExcelMapper.selectList(queryWrapper);



        return shopExcels;
    }
}
