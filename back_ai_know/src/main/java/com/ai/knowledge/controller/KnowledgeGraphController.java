package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.dto.*;
import com.ai.knowledge.entity.Comment;
import com.ai.knowledge.entity.GraphEdge;
import com.ai.knowledge.entity.GraphNode;
import com.ai.knowledge.entity.KnowledgeGraph;
import com.ai.knowledge.service.KnowledgeGraphService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@Tag(name = "知识图谱管理")
public class KnowledgeGraphController {
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    
    @PostMapping("/create")
    @Operation(summary = "创建知识图谱")
    public Result<KnowledgeGraph> createGraph(@RequestBody GraphRequest request) {
        KnowledgeGraph graph = new KnowledgeGraph();
        graph.setName(request.getName());
        graph.setDescription(request.getDescription());
        graph.setCategoryId(request.getCategoryId());
        graph.setOwnerId(request.getOwnerId());
        graph.setIsPublic(request.getIsPublic());
        graph.setCoverImage(request.getCoverImage());
        
        boolean success = knowledgeGraphService.createGraph(graph);
        if (!success) {
            return Result.error("创建知识图谱失败");
        }
        
        return Result.success(graph);
    }
    
    @PostMapping("/node/create")
    @Operation(summary = "创建节点")
    public Result<Void> createNode(@RequestBody NodeRequest request) {
        GraphNode node = new GraphNode();
        node.setGraphId(request.getGraphId());
        node.setNodeId(request.getNodeId());
        node.setName(request.getName());
        node.setCategory(request.getCategory());
        
        boolean success = knowledgeGraphService.createNode(node);
        if (!success) {
            return Result.error("创建节点失败");
        }
        
        return Result.success(null);
    }
    
    @PostMapping("/node/update")
    @Operation(summary = "更新节点")
    public Result<Void> updateNode(@RequestBody NodeRequest request) {
        GraphNode node = new GraphNode();
        node.setId(request.getId());
        node.setGraphId(request.getGraphId());
        node.setNodeId(request.getNodeId());
        node.setName(request.getName());
        node.setCategory(request.getCategory());
        
        boolean success = knowledgeGraphService.updateNode(node);
        if (!success) {
            return Result.error("更新节点失败");
        }
        
        return Result.success(null);
    }
    
    @PostMapping("/edge/create")
    @Operation(summary = "创建边")
    public Result<Void> createEdge(@RequestBody EdgeRequest request) {
        GraphEdge edge = new GraphEdge();
        edge.setGraphId(request.getGraphId());
        edge.setSourceNodeId(request.getSourceNodeId());
        edge.setTargetNodeId(request.getTargetNodeId());
        edge.setRelation(request.getRelation());
        
        boolean success = knowledgeGraphService.createEdge(edge);
        if (!success) {
            return Result.error("创建边失败");
        }
        
        return Result.success(null);
    }
    
    @PostMapping("/update")
    @Operation(summary = "更新知识图谱")
    public Result<Void> updateGraph(@RequestBody GraphRequest request) {
        KnowledgeGraph graph = new KnowledgeGraph();
        graph.setId(request.getId());
        graph.setName(request.getName());
        graph.setDescription(request.getDescription());
        graph.setCategoryId(request.getCategoryId());
        graph.setIsPublic(request.getIsPublic());
        graph.setCoverImage(request.getCoverImage());
        
        boolean success = knowledgeGraphService.updateGraph(graph);
        if (!success) {
            return Result.error("更新知识图谱失败");
        }
        
        return Result.success(null);
    }
    
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除知识图谱")
    public Result<Void> deleteGraph(@PathVariable Long id) {
        boolean success = knowledgeGraphService.deleteGraph(id);
        if (!success) {
            return Result.error("删除知识图谱失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取知识图谱详情")
    public Result<KnowledgeGraph> getGraphById(@PathVariable Long id) {
        KnowledgeGraph graph = knowledgeGraphService.getGraphById(id);
        if (graph == null) {
            return Result.error("知识图谱不存在");
        }
        return Result.success(graph);
    }
    
    @GetMapping("/{id}/nodes")
    @Operation(summary = "获取图谱节点")
    public Result<List<GraphNode>> getNodes(@PathVariable Long id) {
        List<GraphNode> nodes = knowledgeGraphService.getNodes(id);
        return Result.success(nodes);
    }
    
    @GetMapping("/{id}/edges")
    @Operation(summary = "获取图谱边")
    public Result<List<GraphEdge>> getEdges(@PathVariable Long id) {
        List<GraphEdge> edges = knowledgeGraphService.getEdges(id);
        return Result.success(edges);
    }
    
    @GetMapping("/public")
    @Operation(summary = "获取公开知识图谱列表")
    public Result<IPage<KnowledgeGraph>> getPublicGraphs(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Long categoryId) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> result = knowledgeGraphService.getPublicGraphs(pageParam, categoryId);
        return Result.success(result);
    }
    
    @GetMapping("/my")
    @Operation(summary = "获取我的知识图谱列表")
    public Result<IPage<KnowledgeGraph>> getMyGraphs(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam Long userId) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> result = knowledgeGraphService.getMyGraphs(pageParam, userId);
        return Result.success(result);
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索知识图谱")
    public Result<IPage<KnowledgeGraph>> searchGraphs(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Long categoryId) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> result = knowledgeGraphService.searchGraphs(pageParam, keyword, categoryId);
        return Result.success(result);
    }
    
    @PostMapping("/like")
    @Operation(summary = "点赞知识图谱")
    public Result<Void> likeGraph(@RequestBody LikeRequest request) {
        boolean success = knowledgeGraphService.likeGraph(request.getGraphId(), request.getUserId());
        if (!success) {
            return Result.error("点赞失败");
        }
        return Result.success(null);
    }
    
    @PostMapping("/unlike")
    @Operation(summary = "取消点赞")
    public Result<Void> unlikeGraph(@RequestBody LikeRequest request) {
        boolean success = knowledgeGraphService.unlikeGraph(request.getGraphId(), request.getUserId());
        if (!success) {
            return Result.error("取消点赞失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/like/check")
    @Operation(summary = "检查点赞状态")
    public Result<Boolean> isLiked(@RequestParam Long graphId, @RequestParam Long userId) {
        boolean isLiked = knowledgeGraphService.isLiked(graphId, userId);
        return Result.success(isLiked);
    }
    
    @PostMapping("/download")
    @Operation(summary = "下载知识图谱")
    public Result<Void> downloadGraph(@RequestBody DownloadRequest request) {
        boolean success = knowledgeGraphService.downloadGraph(request.getGraphId(), request.getUserId());
        if (!success) {
            return Result.error("下载失败，积分不足");
        }
        return Result.success(null);
    }
    
    @PostMapping("/comment")
    @Operation(summary = "添加评论")
    public Result<Void> addComment(@RequestBody CommentRequest request) {
        boolean success = knowledgeGraphService.addComment(
            request.getGraphId(),
            request.getUserId(),
            request.getContent()
        );
        if (!success) {
            return Result.error("添加评论失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/comments/{graphId}")
    @Operation(summary = "获取评论列表")
    public Result<List<Comment>> getComments(@PathVariable Long graphId) {
        List<Comment> comments = knowledgeGraphService.getComments(graphId);
        return Result.success(comments);
    }
    
    @DeleteMapping("/comment/{commentId}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        boolean success = knowledgeGraphService.deleteComment(commentId, userId);
        if (!success) {
            return Result.error("删除评论失败");
        }
        return Result.success(null);
    }
    
    @GetMapping("/hot")
    @Operation(summary = "获取热门知识图谱")
    public Result<IPage<KnowledgeGraph>> getHotGraphs(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
        
        Page<KnowledgeGraph> pageParam = new Page<>(page, size);
        IPage<KnowledgeGraph> result = knowledgeGraphService.getHotGraphs(pageParam);
        return Result.success(result);
    }
    
    @GetMapping("/hot/top")
    @Operation(summary = "获取前10热门知识图谱")
    public Result<List<KnowledgeGraph>> getTopHotGraphs() {
        List<KnowledgeGraph> result = knowledgeGraphService.getTopHotGraphs(10);
        return Result.success(result);
    }
}