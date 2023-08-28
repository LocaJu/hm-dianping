package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;


    private static final String queueName="stream.orders";


    @Override
    public Result seckillVoucher(Long voucherId) {

        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }


        //获取用户
        Long userId = UserHolder.getUser().getId();
        //返回订单id
        long orderId = redisIdWorker.nextId("order");
        //1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),//没有key,直接传空集合
                voucherId.toString(), userId.toString(),String.valueOf(orderId));

        //2.判断是否为零，即是否有下单资格
        int r = result.intValue();
        if(r!=0){
            //3.不为0，没有下单资格
            return Result.fail(r==1?"库存不足":"不能重复下单");
        }
//        //获取代理对象
//        proxy = (IVoucherOrderService)AopContext.currentProxy();
        return Result.ok(orderId);
    }



    //定义lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {//静态对象在静态代码块中进行初始化
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }




    //定义一个线程池来处理消息队列中的任务
    private static final ExecutorService SECKILL_ORDER_EXECUTOR= Executors.newSingleThreadExecutor();
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    //创建线程任务，处理消息队列中的任务
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    //createVoucherOrder(voucherOrder);
                    save(voucherOrder);
                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }
        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create("stream.orders", ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    //createVoucherOrder(voucherOrder);
                    save(voucherOrder);

                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }


    //
/*
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //获取用户id
        Long userId = voucherOrder.getUserId();
        //获取优惠券Id
        Long voucherId = voucherOrder.getVoucherId();

        //查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //判断是否存在
        if(count>0){
            //用户订单已存在，返回操作错误
            log.error("用户已经领过优惠券");
            return;
        }

        //扣减库存  高并发下的乐观锁解决超卖的问题
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")  //set stock=stock-1
                .eq("voucher_id", voucherId)  //where voucher_id =voucherId
                .gt("stock",0)//乐观锁中判断条件剩余票数大于0  and stock>0
                .update();
        if(!success){
            //扣减失败
            log.error("库存不足！");
            return;
        }

        //抢购优惠券成功
        //创建订单
        //将订单信息存入数据库
        save(voucherOrder);
    }
*/

    /*
    //定义阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    //创建线程任务，处理阻塞队列中的消息
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while(true){
                //获取订单中的信息
                try {
                    VoucherOrder voucherOrder = orderTasks.take();//阻塞队列中如果没有信息会阻塞而不会一直进行while循环
                    //处理订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }
            }
        }
    }


    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //代理添加订单的事务
        proxy.createVoucherOrder(voucherOrder);
    }




    */







    /*
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }


        //获取用户
        Long userId = UserHolder.getUser().getId();
        //1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),//没有key,直接穿空集合
                voucherId.toString(), userId.toString());

        //2.判断是否为零，即是否有下单资格
        int r = result.intValue();
        if(r!=0){
            //3.不为0，没有下单资格
            return Result.fail(r==1?"库存不足":"不能重复下单");
        }

        //4.为0，有下单资格,将下单信息放到阻塞队列
        VoucherOrder voucherOrder=new VoucherOrder();
        //返回订单id
        long orderId = redisIdWorker.nextId("order");
        //订单Id
        voucherOrder.setId(orderId);
        //用户id
        voucherOrder.setVoucherId(voucherId);
        //优惠券id
        voucherOrder.setUserId(userId);

        //放入阻塞队列
        orderTasks.add(voucherOrder);

        //获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();

        return Result.ok(orderId);
    }
    */










   /* @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }


        Long userId = UserHolder.getUser().getId();
        //方法一
//        synchronized (userId.toString().intern()){
//            //获取代理对象（事务），，，由bean管理的事务对象createVoucherOrder
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }



        //方法二：redis分布式锁(自定义实现)
//        SimpleRedisLock simpleRedisLock=new SimpleRedisLock("order:"+userId,stringRedisTemplate);
//       //获取锁
//        boolean lock = simpleRedisLock.tryLock(5);//请求超时时间设置为5second
//        if(!lock){
//            return Result.fail("不能重复下单！");
//        }
//        //获取代理对象（事务），，，由bean管理的事务对象createVoucherOrder
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
////            释放锁🔒
//            simpleRedisLock.unlock();
//        }

        //方法三，redisson实现分布式锁(可重入)
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        //获取锁
        boolean isLock = lock.tryLock();

        if(!isLock){
            return Result.fail("不能重复下单！");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
//            释放锁🔒
            lock.unlock();
        }

    }
*/
/*
    //一人一单秒杀功能高并发下的事务
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //一人一单
        Long userId = UserHolder.getUser().getId();
        //查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //判断是否存在
        if(count>0){
            //用户订单已存在，返回操作错误
            return Result.fail("用户已经领过优惠券");
        }

        //扣减库存  高并发下的乐观锁解决超卖的问题
        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")  //set stock=stock-1
                .eq("voucher_id", voucherId)  //where voucher_id =voucherId
                .gt("stock",0)//乐观锁中判断条件剩余票数大于0  and stock>0
                .update();
        if(!success){
            //扣减失败
            return Result.fail("库存不足！");
        }

        //抢购优惠券成功
        //创建订单
        VoucherOrder voucherOrder=new VoucherOrder();
        //订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //用户id
        //Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        //代金券id
        voucherOrder.setVoucherId(voucherId);
        //将订单信息存入数据库
        save(voucherOrder);

        return Result.ok(orderId);
    }
    */
}
