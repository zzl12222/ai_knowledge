package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.entity.KnowledgeGraph;
import com.ai.knowledge.entity.User;
import com.ai.knowledge.service.KnowledgeGraphService;
import com.ai.knowledge.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "后台管理")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    
    @GetMapping("/users")
    @Operation(summary = "获取所有用户")
    public Result<IPage<User>> getAllUsers(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
        
        Page<User> pageParam = new Page<>(page, size);
        IPage<User> users = userService.page(pageParam);
        return Result.success(users);
    }
    
    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
    
    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        if (!success) {
            return Result.error("删除用户失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/graphs")
    @Operation(summary = "获取所有知识图谱")
    public Result<IPage<KnowledgeGraph>> getAllGraphs(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> graphs = knowledgeGraphService.page(pageParam);
        return Result.success(graphs);
    }
    
    @DeleteMapping("/graphs/{id}")
    @Operation(summary = "删除知识图谱")
    public Result<Void> deleteGraph(@PathVariable Long id) {
        boolean success = knowledgeGraphService.removeById(id);
        if (!success) {
            return Result.error("删除知识图谱失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息")
    public Result<Map<String, Object>> getSystemStats() {
        long userCount = userService.count();
        long graphCount = knowledgeGraphService.count();
        
        Map<String, Object> stats = Map.of(
            "userCount", userCount,
            "graphCount", graphCount
        );
        
        return Result.success(stats);
    }
    
    @GetMapping("/hot/graphs")
    @Operation(summary = "获取热门知识图谱")
    public Result<IPage<KnowledgeGraph>> getHotGraphs(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> graphs = knowledgeGraphService.getHotGraphs(pageParam);
        return Result.success(graphs);
    }
}