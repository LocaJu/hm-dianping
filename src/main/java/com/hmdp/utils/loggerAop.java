package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class loggerAop {

    //private final Logger logger=LoggerFactory.getLogger(this.getClass());

    //重用切入点表达式
    @Pointcut(value = "execution(* com.hmdp.controller.*.*(..))")
    public void pointCut() {}


//    @Before(value = "pointCut()")
//    public void afterMethod(JoinPoint joinPoint) {
//        String methodName = joinPoint.getSignature().getName();
//        logger.debug("后置通知--->"+methodName);
//
//        logger.info("后置通知");
//        System.out.println("Logger-->后置通知，方法名称："+methodName);
//    }

    //最终通知 ：无论目标方法是否出现异常 最终通知都会 执行，将返回结果记录到日志中
    @AfterReturning(returning = "result",pointcut = "pointCut()")
    public void doAfterReturn(JoinPoint joinPoint,Object result) {
        String methodName = joinPoint.getSignature().getName();
        //logger.info("方法:"+methodName+ "Result : {}", result);   private final Logger logger=LoggerFactory.getLogger(this.getClass());
        //log.info("方法:"+methodName+ "Result : {}", result);  // @Slf4j
    }
}
