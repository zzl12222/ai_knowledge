package com.ai.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhipu.api")
public class ZhipuApiConfig {
    private String key;
    private String url;
    private String model;
    private Integer maxTokens;
    private Double temperature;
    private Long timeout;
    private Long connectTimeout;
    private Backup backup;
    
    @Data
    public static class Backup {
        private Boolean enabled;
        private String url;
        private String model;
        private String key;
    }
}
