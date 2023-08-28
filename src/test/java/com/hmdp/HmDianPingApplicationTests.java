package com.hmdp;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopExcel;
import com.hmdp.mapper.ShopExcelMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {



@Test
public static void main(String[] args) {
    Random random = new Random();
    int i = random.nextInt(10);
    System.out.println(i);
}

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService= Executors.newFixedThreadPool(300);
    @Test
    public void test() throws InterruptedException {
        CountDownLatch countDownLatch=new CountDownLatch(100);
        Runnable task=()->{
            for (int i = 0; i < 100; i++) {
                long id=redisIdWorker.nextId("order");
                System.out.println("id="+id);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            executorService.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }



    @Autowired
    StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;
    @Test
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号
        // 2.1.获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        System.out.println(count);
        // 3.拼接并返回
        System.out.println(timestamp);
        return timestamp << COUNT_BITS | count;
    }

    @Test
    public void testNextId(){
        long order = nextId("order");
        System.out.println(order);
    }

    @Resource
    private IShopService shopService;

    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

    @Test
    public void pointtest(){

         String key="point1";
        Double x=120.149192;
        Double y=30.316078;
        String s = String.valueOf(new Point(x, y));
        System.out.println(s);
        GeoReference<Object> objectGeoReference = GeoReference.fromCoordinate(x, y);

        String s1 = objectGeoReference.toString();
        String s2 = String.valueOf(objectGeoReference);
        System.out.println(s1);
        System.out.println(s2);
        System.out.println(objectGeoReference);

    }

    @Resource
    private ServiceImpl<ShopExcelMapper, ShopExcel> mapperShopService;
    @Resource
    private ShopExcelMapper shopExcelMapper;

    @Test
    void test4(){

//        List<Object> objects = mapperShopService.listObjs();
//        String sqlSelect = mapperShopService.query().select("name", "area", "address", "avg_price", "sold").;

        QueryWrapper<ShopExcel> ShopExcelQueryWrapper = new QueryWrapper<>();
        ShopExcelQueryWrapper.select("id","name", "area", "address", "avg_price", "sold");
        List<ShopExcel> shopExcels = shopExcelMapper.selectList(ShopExcelQueryWrapper);
        List<Object> objects = BeanUtil.copyToList(shopExcels, Object.class);
        objects.forEach(System.out::println);

    }
    @Test
    public void test5(){
        String key1="follows:1010";
        String key2="follows:1";
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        System.out.println(intersect);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test6(){
        //String s = stringRedisTemplate.opsForValue().get("sign:1010:202304");
        redisTemplate.opsForValue().set("cache:shopTypeList2:","111");
    }

    @Test
    void test7(){
        UserDTO userDTO=new UserDTO();
        System.out.println(userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        Set<String> strings = userMap.keySet();
        Iterator<String> iterator = strings.iterator();
        while (iterator.hasNext()){
            String k = iterator.next();
            Object v = userMap.get(k);
            System.out.println(k+" "+v);
        }
    }

}


