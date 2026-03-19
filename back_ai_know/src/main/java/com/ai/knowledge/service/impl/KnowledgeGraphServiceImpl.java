package com.ai.knowledge.service.impl;

import com.ai.knowledge.entity.*;
import com.ai.knowledge.mapper.*;
import com.ai.knowledge.service.KnowledgeGraphService;
import com.ai.knowledge.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class KnowledgeGraphServiceImpl extends ServiceImpl<KnowledgeGraphMapper, KnowledgeGraph> 
    implements KnowledgeGraphService {
    
    @Autowired
    private KnowledgeGraphMapper knowledgeGraphMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GraphNodeMapper graphNodeMapper;
    
    @Autowired
    private GraphEdgeMapper graphEdgeMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private LikeMapper likeMapper;
    
    @Autowired
    private HotRecordMapper hotRecordMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String HOT_KEY_PREFIX = "hot:graph:";
    private static final String HOT_CATEGORY_PREFIX = "hot:category:";
    private static final String LIKE_KEY_PREFIX = "like:graph:";
    private static final long HOT_EXPIRE_DAYS = 1;
    
    @Override
    @Transactional
    public boolean createGraph(KnowledgeGraph graph) {
        return knowledgeGraphMapper.insert(graph) > 0;
    }
    
    @Override
    @Transactional
    public boolean createNode(GraphNode node) {
        return graphNodeMapper.insert(node) > 0;
    }
    
    @Override
    @Transactional
    public boolean updateNode(GraphNode node) {
        return graphNodeMapper.updateById(node) > 0;
    }
    
    @Override
    @Transactional
    public boolean createEdge(GraphEdge edge) {
        return graphEdgeMapper.insert(edge) > 0;
    }
    
    @Override
    public List<GraphNode> getNodes(Long graphId) {
        LambdaQueryWrapper<GraphNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphNode::getGraphId, graphId);
        return graphNodeMapper.selectList(wrapper);
    }
    
    @Override
    public List<GraphEdge> getEdges(Long graphId) {
        LambdaQueryWrapper<GraphEdge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphEdge::getGraphId, graphId);
        return graphEdgeMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public boolean updateGraph(KnowledgeGraph graph) {
        return knowledgeGraphMapper.updateById(graph) > 0;
    }
    
    @Override
    @Transactional
    public boolean deleteGraph(Long id) {
        return knowledgeGraphMapper.deleteById(id) > 0;
    }
    
    @Override
    public KnowledgeGraph getGraphById(Long id) {
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(id);
        if (graph != null) {
            graph.setViewCount(graph.getViewCount() + 1);
            knowledgeGraphMapper.updateById(graph);
            updateHotScore(id);
        }
        return graph;
    }
    
    @Override
    public IPage<KnowledgeGraph> getPublicGraphs(Page<KnowledgeGraph> page, Long categoryId) {
        LambdaQueryWrapper<KnowledgeGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeGraph::getIsPublic, 1);
        if (categoryId != null) {
            wrapper.eq(KnowledgeGraph::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KnowledgeGraph::getCreatedAt);
        return knowledgeGraphMapper.selectPage(page, wrapper);
    }
    
    @Override
    public IPage<KnowledgeGraph> getMyGraphs(Page<KnowledgeGraph> page, Long userId) {
        LambdaQueryWrapper<KnowledgeGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeGraph::getOwnerId, userId);
        wrapper.orderByDesc(KnowledgeGraph::getCreatedAt);
        return knowledgeGraphMapper.selectPage(page, wrapper);
    }
    
    @Override
    public IPage<KnowledgeGraph> searchGraphs(Page<KnowledgeGraph> page, String keyword, Long categoryId) {
        LambdaQueryWrapper<KnowledgeGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeGraph::getIsPublic, 1);
        wrapper.and(w -> w.like(KnowledgeGraph::getName, keyword)
            .or().like(KnowledgeGraph::getDescription, keyword));
        if (categoryId != null) {
            wrapper.eq(KnowledgeGraph::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KnowledgeGraph::getHotScore);
        return knowledgeGraphMapper.selectPage(page, wrapper);
    }
    
    @Override
    @Transactional
    public boolean likeGraph(Long graphId, Long userId) {
        String likeKey = LIKE_KEY_PREFIX + graphId;
        
        Boolean isLiked = redisTemplate.opsForSet().isMember(likeKey, userId);
        if (Boolean.TRUE.equals(isLiked)) {
            return false;
        }
        
        redisTemplate.opsForSet().add(likeKey, userId);
        redisTemplate.expire(likeKey, 7, TimeUnit.DAYS);
        
        Long likeCount = redisTemplate.opsForSet().size(likeKey);
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        if (graph != null) {
            graph.setLikeCount(likeCount != null ? likeCount.intValue() : 1);
            knowledgeGraphMapper.updateById(graph);
        }
        
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getGraphId, graphId).eq(Like::getUserId, userId);
        Like existLike = likeMapper.selectOne(wrapper);
        
        if (existLike == null) {
            Like like = new Like();
            like.setGraphId(graphId);
            like.setUserId(userId);
            likeMapper.insert(like);
        }
        
        userService.updateUserPoints(userId, 1);
        updateHotScore(graphId);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean unlikeGraph(Long graphId, Long userId) {
        String likeKey = LIKE_KEY_PREFIX + graphId;
        
        Boolean isLiked = redisTemplate.opsForSet().isMember(likeKey, userId);
        if (!Boolean.TRUE.equals(isLiked)) {
            return false;
        }
        
        redisTemplate.opsForSet().remove(likeKey, userId);
        
        Long likeCount = redisTemplate.opsForSet().size(likeKey);
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        if (graph != null) {
            graph.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
            knowledgeGraphMapper.updateById(graph);
        }
        
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getGraphId, graphId).eq(Like::getUserId, userId);
        likeMapper.delete(wrapper);
        
        return true;
    }
    
    @Override
    public boolean isLiked(Long graphId, Long userId) {
        String likeKey = LIKE_KEY_PREFIX + graphId;
        Boolean isLiked = redisTemplate.opsForSet().isMember(likeKey, userId);
        return Boolean.TRUE.equals(isLiked);
    }
    
    @Override
    @Transactional
    public boolean downloadGraph(Long graphId, Long userId) {
        if (!userService.deductPoints(userId, 10)) {
            return false;
        }
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        graph.setDownloadCount(graph.getDownloadCount() + 1);
        knowledgeGraphMapper.updateById(graph);
        
        updateHotScore(graphId);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean addComment(Long graphId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setGraphId(graphId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        graph.setCommentCount(graph.getCommentCount() + 1);
        knowledgeGraphMapper.updateById(graph);
        
        updateHotScore(graphId);
        
        return true;
    }
    
    @Override
    public List<Comment> getComments(Long graphId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getGraphId, graphId);
        wrapper.orderByDesc(Comment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            return false;
        }
        
        commentMapper.deleteById(commentId);
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(comment.getGraphId());
        if (graph != null && graph.getCommentCount() > 0) {
            graph.setCommentCount(graph.getCommentCount() - 1);
            knowledgeGraphMapper.updateById(graph);
        }
        
        return true;
    }
    
    @Override
    public IPage<KnowledgeGraph> getHotGraphs(Page<KnowledgeGraph> page) {
        LambdaQueryWrapper<KnowledgeGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeGraph::getIsPublic, 1);
        wrapper.orderByDesc(KnowledgeGraph::getHotScore);
        return knowledgeGraphMapper.selectPage(page, wrapper);
    }
    
    @Override
    public List<KnowledgeGraph> getTopHotGraphs(int limit) {
        LambdaQueryWrapper<KnowledgeGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeGraph::getIsPublic, 1);
        wrapper.orderByDesc(KnowledgeGraph::getHotScore);
        wrapper.last("LIMIT " + limit);
        return knowledgeGraphMapper.selectList(wrapper);
    }
    
    private void updateHotScore(Long graphId) {
        String key = HOT_KEY_PREFIX + graphId;
        Long score = redisTemplate.opsForValue().increment(key);
        if (score == 1) {
            redisTemplate.expire(key, HOT_EXPIRE_DAYS, TimeUnit.DAYS);
        }
        
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        if (graph != null) {
            int hotScore = graph.getViewCount() * 1 + graph.getLikeCount() * 5 + 
                          graph.getCommentCount() * 3 + graph.getDownloadCount() * 2;
            graph.setHotScore(hotScore);
            knowledgeGraphMapper.updateById(graph);
        }
    }
}