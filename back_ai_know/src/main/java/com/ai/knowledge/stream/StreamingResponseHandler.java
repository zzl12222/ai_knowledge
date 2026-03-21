package com.ai.knowledge.stream;

import com.ai.knowledge.config.ZhipuApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StreamingResponseHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingResponseHandler.class);
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final List<CompletableFuture<Void>> activeTasks = new ArrayList<>();
    
    @Autowired
    private ZhipuApiConfig zhipuApiConfig;
    
    public interface StreamingCallback {
        void onChunk(String chunk);
        String onComplete(String fullResponse);
        void onError(Throwable error);
    }
    
    public void handleStreamingResponse(
            String apiUrl,
            Map<String, Object> requestBody,
            StreamingCallback callback,
            Long timeoutMs,
            boolean isKnowledgeGraph
    ) {
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            boolean completed = false;
            try {
                cn.hutool.http.HttpResponse httpResponse = cn.hutool.http.HttpRequest.post(apiUrl)
                        .header("Authorization", "Bearer " + zhipuApiConfig.getKey())
                        .header("Content-Type", "application/json")
                        .body(new cn.hutool.json.JSONObject(requestBody).toString())
                        .timeout((int) (timeoutMs != null ? timeoutMs : zhipuApiConfig.getTimeout()))
                        .setConnectionTimeout(zhipuApiConfig.getConnectTimeout().intValue())
                        .execute();
                
                logger.info("AI API响应状态: {}", httpResponse.getStatus());
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.bodyStream()));
                String line;
                StringBuilder fullResponse = new StringBuilder();
                
                while ((line = reader.readLine()) != null && !completed) {
                    logger.debug("收到AI API行: {}", line);
                    
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if (data.equals("[DONE]")) {
                            logger.info("流式响应完成");
                            break;
                        }
                        
                        try {
                            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(data);
                            if (json.containsKey("choices")) {
                                String content = json.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("delta")
                                        .getStr("content");
                                
                                if (content != null && !content.isEmpty()) {
                                    fullResponse.append(content);
                                    logger.debug("发送流式数据: {}", content);
                                    callback.onChunk(content);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("解析流式数据失败: {}", e.getMessage());
                        }
                    }
                }
                
                reader.close();
                String finalResponse = fullResponse.toString();
                logger.info("流式响应最终完成，总长度: {}", finalResponse.length());
                completed = true;
                String result = callback.onComplete(finalResponse);
                logger.info("回调返回结果: {}", result);
                
            } catch (Exception e) {
                logger.error("流式响应异常: {}", e.getMessage(), e);
                if (!completed) {
                    callback.onError(e);
                }
            }
        }, executorService);
        
        activeTasks.add(task);
        
        task.whenComplete((result, error) -> {
            activeTasks.remove(task);
            if (error != null) {
                logger.error("流式任务异常完成: {}", error.getMessage());
            }
        });
    }
    
    public void shutdown() {
        logger.info("关闭流式响应处理器，取消 {} 个活跃任务", activeTasks.size());
        for (CompletableFuture<Void> task : activeTasks) {
            task.cancel(true);
        }
        executorService.shutdown();
    }
    
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
}
