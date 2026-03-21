package com.ai.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "validation")
public class ValidationConfig {
    private Integer maxRetries = 3;
}
