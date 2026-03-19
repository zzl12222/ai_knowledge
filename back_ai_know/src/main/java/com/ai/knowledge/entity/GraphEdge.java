package com.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("graph_edges")
public class GraphEdge extends Model {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long graphId;
    
    private String sourceNodeId;
    
    private String targetNodeId;
    
    private String relation;
    
    private String properties;
    
    private LocalDateTime createdAt;
}