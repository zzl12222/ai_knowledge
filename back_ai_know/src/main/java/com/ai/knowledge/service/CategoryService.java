package com.ai.knowledge.service;

import com.ai.knowledge.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {
    
    List<Category> getAllCategories();
    
    Category getCategoryById(Long id);
    
    boolean createCategory(Category category);
    
    boolean updateCategory(Category category);
    
    boolean deleteCategory(Long id);
}