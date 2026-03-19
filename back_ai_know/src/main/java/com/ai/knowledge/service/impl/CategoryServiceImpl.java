package com.ai.knowledge.service.impl;

import com.ai.knowledge.entity.Category;
import com.ai.knowledge.mapper.CategoryMapper;
import com.ai.knowledge.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> 
    implements CategoryService {
    
    @Override
    public List<Category> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return list(wrapper);
    }
    
    @Override
    public Category getCategoryById(Long id) {
        return getById(id);
    }
    
    @Override
    public boolean createCategory(Category category) {
        return save(category);
    }
    
    @Override
    public boolean updateCategory(Category category) {
        return updateById(category);
    }
    
    @Override
    public boolean deleteCategory(Long id) {
        return removeById(id);
    }
}