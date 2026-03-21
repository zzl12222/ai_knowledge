package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class NodeRequest {
    private Long id;
    private Long graphId;
    private String nodeId;
    private String name;
    private String category;
}
