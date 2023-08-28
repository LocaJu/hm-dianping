package com.hmdp.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopTypeService typeService;

    @Override
    public Result queryTypeList() {
        //从redis缓存中查询商铺类型列表信息
        String shopTypeListJson = stringRedisTemplate.opsForValue().get("cache:shopTypeList:");
        //不为空，查到信息返回结果
        if(StrUtil.isNotEmpty(shopTypeListJson)){
            List<ShopType> shopTypes = JSONUtil.toList(shopTypeListJson, ShopType.class);
            return Result.ok(shopTypes);
        }
        //在缓存中未查到信息，从数据库中查找
        List<ShopType> typeList = typeService.query().orderByAsc("sort").list();



        //存入缓存，并返回结果
        stringRedisTemplate.opsForValue().set("cache:shopTypeList:",JSONUtil.toJsonStr(typeList));



        return Result.ok(typeList);
    }
}
