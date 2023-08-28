package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.checkCode.CheckCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private HttpServletResponse response;
    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // TODO 发送短信验证码并保存验证码

        return userService.sendCode(phone,session);

       //return Result.fail("功能未完成");
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // TODO 实现登录功能(验证码)
        log.debug("登录参数，包含手机号、验证码"+loginForm.toString());
       return userService.login(loginForm,session);
        //return Result.fail("功能未完成");
    }

    //方法二:账号密码登录
    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、密码
     */
    @PostMapping("/loginPassword")
    public Result login2(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // TODO 实现登录功能(账号密码)
        log.debug("登录参数，包含手机号、密码"+loginForm.toString());
        return userService.login2(loginForm,session);
    }
    /**
     * 生成验证码
     * @throws IOException
     */
    @GetMapping("/checkCodeImage")
    public Result getCheckCodeImage() throws IOException {

        return userService.getCheckCodeImage();

    }


    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token){
        // TODO 实现登出功能

        return userService.logout(token);
//        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回

        UserDTO user = UserHolder.getUser();



        return Result.ok(user);

        //return Result.fail("功能未完成");
    }


    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
//        User user = userService.getById(userId);
//        if(user==null){
//            return Result.ok();
//        }
//        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
//        // 返回
//        return Result.ok(userDTO);

        return userService.queryUserById(userId);
    }

    //用户签到
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    //统计签到结果
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }



    @GetMapping("/editPassword")
    public Result editPassword(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/icon")
    public Result icon(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/nickName")
    public Result nickName(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/birthday")
    public Result birthday(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/city")
    public Result city(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/sex")
    public Result sex(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/scription")
    public Result scription(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/checkvip")
    public Result checkvip(){
        return Result.fail("功能未完成");
    }
    @GetMapping("/points")
    public Result points(){
        return Result.fail("功能未完成");
    }


}
