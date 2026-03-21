package com.ai.knowledge.validator;

import com.ai.knowledge.config.ValidationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class GraphDataValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphDataValidator.class);
    
    private static final Pattern NODE_PATTERN = Pattern.compile(
        "\\{\\s*\"nodes\"\\s*:\\s*\\[\\s*\\{\\s*\"nodeId\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"name\"\\s*:\\s*\"[^\"]+\"\\s*(,\\s*\"category\"\\s*:\\s*\"[^\"]*\")?\\s*\\}\\s*\\]"
    );
    
    private static final Pattern EDGE_PATTERN = Pattern.compile(
        "\\{\\s*\"edges\"\\s*:\\s*\\[\\s*\\{\\s*\"sourceNodeId\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"targetNodeId\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"relation\"\\s*:\\s*\"[^\"]+\"\\s*(,\\s*\"properties\"\\s*:\\s*\"[^\"]*\")?\\s*\\}\\s*\\]"
    );
    
    private static final Pattern GRAPH_PATTERN = Pattern.compile(
        "\\{\\s*\"nodes\"\\s*:\\s*\\[.*?\\]\\s*,\\s*\"edges\"\\s*:\\s*\\[.*?\\]\\s*\\}"
    );
    
    @Autowired
    private ValidationConfig validationConfig;
    
    public boolean validateGraphData(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            logger.warn("验证失败：JSON数据为空");
            return false;
        }
        
        logger.debug("开始验证知识图谱数据: {}", jsonData);
        
        if (!GRAPH_PATTERN.matcher(jsonData).matches()) {
            logger.warn("验证失败：JSON格式不符合知识图谱结构");
            return false;
        }
        
        if (!NODE_PATTERN.matcher(jsonData).find()) {
            logger.warn("验证失败：未找到有效的节点数据");
            return false;
        }
        
        if (!EDGE_PATTERN.matcher(jsonData).find()) {
            logger.warn("验证失败：未找到有效的关系数据");
            return false;
        }
        
        logger.info("知识图谱数据验证通过");
        return true;
    }
    
    public boolean isValidGraphResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            logger.warn("验证失败：响应数据为空");
            return false;
        }
        
        try {
            if (!response.startsWith("{") || !response.endsWith("}")) {
                logger.warn("验证失败：响应不是有效的JSON对象");
                return false;
            }
            
            if (!response.contains("nodes") && !response.contains("edges")) {
                logger.warn("验证失败：响应中缺少nodes或edges字段");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("验证过程中发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    public int getMaxRetries() {
        return validationConfig.getMaxRetries();
    }
}
