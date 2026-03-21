package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
}
