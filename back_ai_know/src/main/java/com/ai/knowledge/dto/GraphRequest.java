package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class GraphRequest {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private Long ownerId;
    private Integer isPublic;
    private String coverImage;
}
