package com.ai.knowledge.dto;

import lombok.Data;

@Data
public class GenerateRequest {
    private String text;
    private Boolean isPublic;
}
