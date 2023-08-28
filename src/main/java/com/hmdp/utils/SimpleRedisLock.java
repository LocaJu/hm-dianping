package com.hmdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import cn.hutool.core.lang.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String lockName;
    private StringRedisTemplate stringRedisTemplate;


    public SimpleRedisLock(String lockName, StringRedisTemplate stringRedisTemplate) {
        this.lockName = lockName;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";


    //释放锁脚本lua
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {//静态对象在静态代码块中进行初始化
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //当前线程名称加UUID作为redis的值即唯一标识
        String threadUUId=ID_PREFIX+Thread.currentThread().getId();
        //获取锁  从redis中获取标识
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + lockName, threadUUId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }



    //lua脚本方法释放锁，（过程的原子性）
    @Override
    public void unlock() {
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,                                    //向lua脚本中传入key和value来进行锁释放（保证释放过程的原子性）
                Collections.singletonList(KEY_PREFIX + lockName),//从redis中获取标识       key
                ID_PREFIX+Thread.currentThread().getId());//当前线程名称加UUID作为redis的值即唯一标识   value
    }


    //释放锁，不能保证过程的原子性
//    @Override
//    public void unlock() {
//        //获取当期线程的标识
//        String threadId=ID_PREFIX+Thread.currentThread().getId();
//        //获取锁中的标识
//        String lockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + lockName);
//
//        //判断当前线程标识和锁(redids)中的标识是否是同一把锁，如果是则进行释放锁操作
//        if(threadId.equals(lockId)){
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + lockName);
//        }
//
//    }
}
