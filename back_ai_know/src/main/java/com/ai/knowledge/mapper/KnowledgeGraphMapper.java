package com.ai.knowledge.mapper;

import com.ai.knowledge.entity.KnowledgeGraph;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface KnowledgeGraphMapper extends BaseMapper<KnowledgeGraph> {
    
    IPage<KnowledgeGraph> selectPublicGraphs(Page<KnowledgeGraph> page, @Param("categoryId") Long categoryId);
    
    IPage<KnowledgeGraph> selectMyGraphs(Page<KnowledgeGraph> page, @Param("userId") Long userId);
    
    IPage<KnowledgeGraph> searchGraphs(Page<KnowledgeGraph> page, @Param("keyword") String keyword, @Param("categoryId") Long categoryId);
    
    IPage<KnowledgeGraph> selectHotGraphs(Page<KnowledgeGraph> page);
}