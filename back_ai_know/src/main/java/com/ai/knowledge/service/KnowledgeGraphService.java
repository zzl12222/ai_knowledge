package com.ai.knowledge.service;

import com.ai.knowledge.entity.Comment;
import com.ai.knowledge.entity.GraphEdge;
import com.ai.knowledge.entity.GraphNode;
import com.ai.knowledge.entity.KnowledgeGraph;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface KnowledgeGraphService extends IService<KnowledgeGraph> {
    
    boolean createGraph(KnowledgeGraph graph);
    
    boolean createNode(GraphNode node);
    
    boolean updateNode(GraphNode node);
    
    boolean createEdge(GraphEdge edge);
    
    List<GraphNode> getNodes(Long graphId);
    
    List<GraphEdge> getEdges(Long graphId);
    
    boolean updateGraph(KnowledgeGraph graph);
    
    boolean deleteGraph(Long id);
    
    KnowledgeGraph getGraphById(Long id);
    
    IPage<KnowledgeGraph> getPublicGraphs(Page<KnowledgeGraph> page, Long categoryId);
    
    IPage<KnowledgeGraph> getMyGraphs(Page<KnowledgeGraph> page, Long userId);
    
    IPage<KnowledgeGraph> searchGraphs(Page<KnowledgeGraph> page, String keyword, Long categoryId);
    
    boolean likeGraph(Long graphId, Long userId);
    
    boolean unlikeGraph(Long graphId, Long userId);
    
    boolean isLiked(Long graphId, Long userId);
    
    boolean downloadGraph(Long graphId, Long userId);
    
    boolean addComment(Long graphId, Long userId, String content);
    
    List<Comment> getComments(Long graphId);
    
    boolean deleteComment(Long commentId, Long userId);
    
    IPage<KnowledgeGraph> getHotGraphs(Page<KnowledgeGraph> page);
    
    List<KnowledgeGraph> getTopHotGraphs(int limit);
}