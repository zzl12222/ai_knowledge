package com.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hot_records")
public class HotRecord extends Model {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long graphId;
    
    private Integer viewCount;
    
    private Integer likeCount;
    
    private Integer commentCount;
    
    private Integer downloadCount;
    
    private LocalDate recordDate;
    
    private LocalDateTime createdAt;
}