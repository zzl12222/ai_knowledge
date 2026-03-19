package com.ai.knowledge.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    
    String generateKnowledgeGraph(String text, boolean isPublic);
    
    List<Map<String, Object>> parseKnowledgeGraph(String text);
    
    String chat(String message, List<Map<String, Object>> history);
    
    String chatWithAutoSave(Long userId, String message);
    
    String chatStream(Long userId, String message);
    
    String chatNonStream(Long userId, String message);
    
    List<Map<String, Object>> getChatHistory(Long userId);
    
    void saveChatHistory(Long userId, List<Map<String, Object>> history);
    
    void clearChatHistory(Long userId);
}