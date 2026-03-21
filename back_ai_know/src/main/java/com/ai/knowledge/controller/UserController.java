package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.dto.*;
import com.ai.knowledge.entity.User;
import com.ai.knowledge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        
        return Result.success(user);
    }
    
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = userService.register(
            request.getUsername(),
            request.getPassword(),
            request.getEmail()
        );
        
        if (user == null) {
            return Result.error("用户名已存在");
        }
        
        return Result.success(user);
    }
    
    @GetMapping("/info/{id}")
    @Operation(summary = "获取用户信息")
    public Result<User> getUserInfo(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
    
    @PostMapping("/points/update")
    @Operation(summary = "更新用户积分")
    public Result<Void> updatePoints(@RequestBody PointsRequest request) {
        boolean success = userService.updateUserPoints(request.getUserId(), request.getPoints());
        if (!success) {
            return Result.error("更新积分失败");
        }
        return Result.success(null);
    }
    
    @PostMapping("/points/deduct")
    @Operation(summary = "扣除用户积分")
    public Result<Void> deductPoints(@RequestBody PointsRequest request) {
        boolean success = userService.deductPoints(request.getUserId(), request.getPoints());
        if (!success) {
            return Result.error("积分不足");
        }
        return Result.success(null);
    }
}