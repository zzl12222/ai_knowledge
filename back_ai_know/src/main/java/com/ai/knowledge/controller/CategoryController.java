package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.entity.Category;
import com.ai.knowledge.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@Tag(name = "分类管理")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @GetMapping("/list")
    @Operation(summary = "获取所有分类")
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        return Result.success(category);
    }
    
    @PostMapping("/create")
    @Operation(summary = "创建分类")
    public Result<Category> createCategory(@RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        
        boolean success = categoryService.createCategory(category);
        if (!success) {
            return Result.error("创建分类失败");
        }
        
        return Result.success(category);
    }
    
    @PostMapping("/update")
    @Operation(summary = "更新分类")
    public Result<Void> updateCategory(@RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setId(request.getId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        
        boolean success = categoryService.updateCategory(category);
        if (!success) {
            return Result.error("更新分类失败");
        }
        
        return Result.success(null);
    }
    
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        boolean success = categoryService.deleteCategory(id);
        if (!success) {
            return Result.error("删除分类失败");
        }
        return Result.success(null);
    }
    
    @Data
    public static class CategoryRequest {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private Integer sortOrder;
    }
}