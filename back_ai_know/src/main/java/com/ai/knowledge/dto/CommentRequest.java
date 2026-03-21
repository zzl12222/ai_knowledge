package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long graphId;
    private Long userId;
    private String content;
}
