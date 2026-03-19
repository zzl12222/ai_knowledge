package com.ai.knowledge.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ai.knowledge.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AIServiceImpl implements AIService {
    
    @Value("${zhipu.api.key}")
    private String apiKey;
    
    @Value("${zhipu.api.url}")
    private String apiUrl;
    
    @Value("${zhipu.api.model}")
    private String model;
    
    @Value("${zhipu.api.max-tokens}")
    private Integer maxTokens;
    
    @Value("${zhipu.api.temperature}")
    private Double temperature;
    
    @Value("${zhipu.api.timeout}")
    private Integer timeout;
    
    @Value("${zhipu.api.connect-timeout}")
    private Integer connectTimeout;
    
    @Value("${zhipu.api.backup.enabled:false}")
    private Boolean backupEnabled;
    
    @Value("${zhipu.api.backup.url:}")
    private String backupUrl;
    
    @Value("${zhipu.api.backup.model:}")
    private String backupModel;
    
    @Value("${zhipu.api.backup.key:}")
    private String backupKey;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SYSTEM_PROMPT = "你是一个知识图谱生成助手。请分析用户提供的文本，提取其中的实体（节点）和关系（边），并以JSON格式返回。" +
        "返回格式要求：{\"nodes\":[{\"id\":\"节点ID\",\"name\":\"节点名称\",\"category\":\"节点类别\"}],\"edges\":[{\"source\":\"起点节点ID\",\"label\":\"关系名称\",\"target\":\"终点节点ID\"}]}。" +
        "返回格式实例：{\"nodes\":[{\"id\":\"java\",\"name\":\"Java\",\"category\":\"编程语言\"},{\"id\":\"spring\",\"name\":\"Spring框架\",\"category\":\"框架\"},{\"id\":\"tomcat\",\"name\":\"Tomcat\",\"category\":\"服务器\"}],\"edges\":[{\"source\":\"java\",\"label\":\"基于\",\"target\":\"spring\"},{\"source\":\"spring\",\"label\":\"运行在\",\"target\":\"tomcat\"}]}。" +
        "节点ID应该是唯一的英文字符串，节点名称是中文描述，节点类别可以是：人物、地点、事件、概念、组织等。" +
        "关系名称应该是简洁的动词或名词短语。" +
        "【重要规则】：" +
        "1. 只返回JSON格式的内容，不要包含任何其他文字说明、解释或格式标记" +
        "2. 确保返回的内容可以直接被JSON解析器解析" +
        "3. 不要添加```json```或其他代码块标记" +
        "4. 不要添加任何解释性文字，如'这是生成的知识图谱'等" +
        "5. 字段名称必须严格按照：nodes数组包含id、name、category；edges数组包含source、label、target" +
        "6. 返回的内容必须以{开始，以}结束，中间不能有其他内容";
    
    private static final String CHAT_SYSTEM_PROMPT = "你是一个智能助手，可以帮助用户解答问题、提供建议和进行日常对话。请用自然、友好的语气回答用户的问题。";
    
    private static final String CHAT_HISTORY_PREFIX = "ai:chat:history:";
    private static final long CHAT_HISTORY_EXPIRE_DAYS = 2;
    
    @Override
    public String generateKnowledgeGraph(String text, boolean isPublic) {
        String prompt;
        if (text.contains("知识图谱")) {
            prompt = SYSTEM_PROMPT + "\n\n请分析以下文本并生成知识图谱：\n" + text;
        } else {
            prompt = CHAT_SYSTEM_PROMPT + "\n\n" + text;
        }
        
        try {
            String response = callZhipuAI(prompt);
            if (text.contains("知识图谱")) {
                response = cleanJsonResponse(response);
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"生成失败\"}";
        }
    }
    
    @Override
    public List<Map<String, Object>> parseKnowledgeGraph(String text) {
        String prompt;
        if (text.contains("知识图谱")) {
            prompt = SYSTEM_PROMPT + "\n\n请分析以下文本并生成知识图谱：\n" + text;
        } else {
            prompt = CHAT_SYSTEM_PROMPT + "\n\n" + text;
        }
        
        try {
            String response = callZhipuAI(prompt);
            
            if (!text.contains("知识图谱")) {
                return Collections.singletonList(Map.of("type", "text", "data", response));
            }
            
            response = cleanJsonResponse(response);
            JSONObject json = JSONUtil.parseObj(response);
            
            List<Map<String, Object>> result = new ArrayList<>();
            if (json.containsKey("nodes")) {
                result.add(Map.of("type", "nodes", "data", json.getJSONArray("nodes")));
            }
            if (json.containsKey("edges")) {
                result.add(Map.of("type", "edges", "data", json.getJSONArray("edges")));
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonList(Map.of("type", "error", "data", "解析失败"));
        }
    }
    
    @Override
    public String chat(String message, List<Map<String, Object>> history) {
        String response = callZhipuAIWithHistory(message, history);
        return response;
    }
    
    @Override
    public String chatWithAutoSave(Long userId, String message) {
        List<Map<String, Object>> history = getChatHistory(userId);
        
        String response;
        try {
            if (message.contains("知识图谱")) {
                response = callZhipuAIWithHistoryAndSystem(message, history, SYSTEM_PROMPT);
                response = cleanJsonResponse(response);
            } else {
                response = callZhipuAIWithHistoryAndSystem(message, history, CHAT_SYSTEM_PROMPT);
            }
            
            history.add(Map.of("role", "user", "content", message));
            history.add(Map.of("role", "assistant", "content", response));
            
            saveChatHistory(userId, history);
            
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI服务暂时不可用，请稍后重试。错误信息：" + e.getMessage();
        }
    }
    
    @Override
    public String chatStream(Long userId, String message) {
        List<Map<String, Object>> history = getChatHistory(userId);
        String systemPrompt = message.contains("知识图谱") ? SYSTEM_PROMPT : CHAT_SYSTEM_PROMPT;
        
        String fullResponse = callZhipuAIStreamWithHistoryAndSystem(message, history, systemPrompt);
        
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", fullResponse));
        
        saveChatHistory(userId, history);
        
        return fullResponse;
    }
    
    @Override
    public String chatNonStream(Long userId, String message) {
        List<Map<String, Object>> history = getChatHistory(userId);
        String systemPrompt = message.contains("知识图谱") ? SYSTEM_PROMPT : CHAT_SYSTEM_PROMPT;
        
        String fullResponse = callZhipuAINonStreamWithHistoryAndSystem(message, history, systemPrompt);
        
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "assistant", "content", fullResponse));
        
        saveChatHistory(userId, history);
        
        return fullResponse;
    }
    
    private String callZhipuAI(String prompt) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        requestBody.set("messages", Collections.singletonList(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.set("max_tokens", maxTokens);
        requestBody.set("temperature", temperature);
        
        HttpResponse httpResponse = HttpRequest.post(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json;charset=UTF-8")
            .body(requestBody.toString())
            .timeout(timeout)
            .setConnectionTimeout(connectTimeout)
            .execute();
        
        String responseBody = httpResponse.body();
        JSONObject responseJson = JSONUtil.parseObj(responseBody);
        
        if (responseJson.containsKey("choices")) {
            return responseJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getStr("content");
        }
        
        return "AI响应失败";
    }
    
    private String callZhipuAIWithHistory(String message, List<Map<String, Object>> history) {
        return callZhipuAIWithHistoryAndSystem(message, history, null);
    }
    
    private String callZhipuAIWithHistoryAndSystem(String message, List<Map<String, Object>> history, String systemPrompt) {
        return callZhipuAIWithHistoryAndSystem(message, history, systemPrompt, 3);
    }
    
    private String callZhipuAIWithHistoryAndSystem(String message, List<Map<String, Object>> history, String systemPrompt, int retryCount) {
        int maxRetries = retryCount;
        int currentRetry = 0;
        Exception lastException = null;
        
        while (currentRetry <= maxRetries) {
            try {
                List<Map<String, Object>> messages = new ArrayList<>();
                
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                }
                
                if (history != null && !history.isEmpty()) {
                    messages.addAll(history);
                }
                
                messages.add(Map.of("role", "user", "content", message));
                
                JSONObject requestBody = new JSONObject();
                requestBody.set("model", model);
                requestBody.set("messages", messages);
                requestBody.set("max_tokens", maxTokens);
                requestBody.set("temperature", temperature);
                
                HttpResponse httpResponse = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(requestBody.toString())
                    .timeout(timeout)
                    .setConnectionTimeout(connectTimeout)
                    .execute();
                
                String responseBody = httpResponse.body();
                JSONObject responseJson = JSONUtil.parseObj(responseBody);
                
                if (responseJson.containsKey("choices")) {
                    return responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");
                }
                
                return "AI响应失败";
            } catch (Exception e) {
                lastException = e;
                currentRetry++;
                
                if (currentRetry <= maxRetries) {
                    try {
                        Thread.sleep(1000 * currentRetry);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return "AI响应失败，请稍后重试。错误：" + (lastException != null ? lastException.getMessage() : "未知错误");
    }
    
    private String callZhipuAINonStreamWithHistoryAndSystem(String message, List<Map<String, Object>> history, String systemPrompt) {
        int maxRetries = 3;
        int currentRetry = 0;
        Exception lastException = null;
        
        while (currentRetry <= maxRetries) {
            try {
                List<Map<String, Object>> messages = new ArrayList<>();
                
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                }
                
                if (history != null && !history.isEmpty()) {
                    messages.addAll(history);
                }
                
                messages.add(Map.of("role", "user", "content", message));
                
                JSONObject requestBody = new JSONObject();
                requestBody.set("model", model);
                requestBody.set("messages", messages);
                requestBody.set("max_tokens", maxTokens);
                requestBody.set("temperature", temperature);
                
                HttpResponse httpResponse = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(requestBody.toString())
                    .timeout(timeout)
                    .setConnectionTimeout(connectTimeout)
                    .execute();
                
                String responseBody = httpResponse.body();
                JSONObject responseJson = JSONUtil.parseObj(responseBody);
                
                if (responseJson.containsKey("choices")) {
                    return responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");
                }
                
                return "AI响应失败";
            } catch (Exception e) {
                lastException = e;
                currentRetry++;
                
                System.err.println("AI请求失败，第 " + currentRetry + " 次重试，错误: " + e.getMessage());
                
                if (currentRetry <= maxRetries) {
                    try {
                        Thread.sleep(2000 * currentRetry);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return "AI响应失败，请稍后重试。错误：" + (lastException != null ? lastException.getMessage() : "未知错误");
    }
    
    private String callZhipuAIStreamWithHistoryAndSystem(String message, List<Map<String, Object>> history, String systemPrompt) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        
        messages.add(Map.of("role", "user", "content", message));
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", model);
        requestBody.set("messages", messages);
        requestBody.set("max_tokens", maxTokens);
        requestBody.set("temperature", temperature);
        requestBody.set("stream", true);
        
        StringBuilder fullResponse = new StringBuilder();
        
        try {
            HttpResponse httpResponse = HttpRequest.post(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(requestBody.toString())
                .timeout(timeout)
                .setConnectionTimeout(connectTimeout)
                .execute();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.bodyStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    
                    try {
                        JSONObject json = JSONUtil.parseObj(data);
                        if (json.containsKey("choices")) {
                            String content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                                .getStr("content");
                            if (content != null && !content.isEmpty()) {
                                fullResponse.append(content);
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            reader.close();
            
            return fullResponse.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI响应失败，请稍后重试。错误：" + e.getMessage();
        }
    }
    
    public void saveChatHistory(Long userId, List<Map<String, Object>> history) {
        String key = CHAT_HISTORY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, history, CHAT_HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
    }
    
    public List<Map<String, Object>> getChatHistory(Long userId) {
        String key = CHAT_HISTORY_PREFIX + userId;
        Object history = redisTemplate.opsForValue().get(key);
        if (history != null) {
            return (List<Map<String, Object>>) history;
        }
        return new ArrayList<>();
    }
    
    @Override
    public void clearChatHistory(Long userId) {
        String key = CHAT_HISTORY_PREFIX + userId;
        redisTemplate.delete(key);
    }
    
    private String cleanJsonResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "{\"nodes\":[],\"edges\":[]}";
        }
        
        String cleaned = response.trim();
        
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }
        
        cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        
        try {
            JSONUtil.parseObj(cleaned);
            return cleaned;
        } catch (Exception e) {
            System.err.println("JSON解析失败，返回默认空图谱: " + e.getMessage());
            return "{\"nodes\":[],\"edges\":[]}";
        }
    }
}