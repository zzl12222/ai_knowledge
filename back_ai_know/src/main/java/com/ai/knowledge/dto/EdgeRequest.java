package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class EdgeRequest {
    private Long graphId;
    private String sourceNodeId;
    private String targetNodeId;
    private String relation;
}
