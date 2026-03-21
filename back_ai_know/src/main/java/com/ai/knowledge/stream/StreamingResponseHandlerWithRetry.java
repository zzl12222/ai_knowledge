package com.ai.knowledge.stream;

import com.ai.knowledge.config.ZhipuApiConfig;
import com.ai.knowledge.validator.GraphDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StreamingResponseHandlerWithRetry {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingResponseHandlerWithRetry.class);
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final int maxRetries;
    private final long retryDelayMs;
    private final int activeTasks = new java.util.concurrent.atomic.AtomicInteger(0).get();
    
    @Autowired
    private ZhipuApiConfig zhipuApiConfig;
    
    @Autowired
    private GraphDataValidator graphDataValidator;
    
    public interface StreamingCallback {
        void onChunk(String chunk);
        void onComplete(String fullResponse);
        void onError(Throwable error);
        void onRetry(int attempt, Throwable lastError);
    }
    
    public StreamingResponseHandlerWithRetry(int maxRetries, long retryDelayMs) {
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    public StreamingResponseHandlerWithRetry() {
        this(3, 2000);
    }
    
    public void handleStreamingResponseWithRetry(
            String apiUrl,
            Map<String, Object> requestBody,
            StreamingCallback callback,
            Long timeoutMs
    ) {
        handleStreamingResponseWithRetry(apiUrl, requestBody, callback, timeoutMs, 0);
    }
    
    private void handleStreamingResponseWithRetry(
            String apiUrl,
            Map<String, Object> requestBody,
            StreamingCallback callback,
            Long timeoutMs,
            int attempt
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("尝试第 {} 次流式请求...", attempt + 1);
                
                cn.hutool.http.HttpResponse httpResponse = cn.hutool.http.HttpRequest.post(apiUrl)
                        .header("Authorization", "Bearer " + zhipuApiConfig.getKey())
                        .header("Content-Type", "application/json")
                        .body(new cn.hutool.json.JSONObject(requestBody).toString())
                        .timeout((int) (timeoutMs != null ? timeoutMs : zhipuApiConfig.getTimeout()))
                        .setConnectionTimeout(zhipuApiConfig.getConnectTimeout().intValue())
                        .execute();
                
                logger.info("AI API响应状态: {}", httpResponse.getStatus());
                
                if (httpResponse.getStatus() != 200) {
                    throw new RuntimeException("AI API返回错误状态: " + httpResponse.getStatus());
                }
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(httpResponse.bodyStream())
                );
                String line;
                StringBuilder fullResponse = new StringBuilder();
                boolean completed = false;
                
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
                
                if (!graphDataValidator.isValidGraphResponse(finalResponse)) {
                    logger.warn("知识图谱数据验证失败，准备重试");
                    throw new RuntimeException("返回的知识图谱数据格式不正确");
                }
                
                String result = callback.onComplete(finalResponse);
                logger.info("回调返回结果: {}", result);
                
            } catch (Exception e) {
                logger.error("流式请求失败 (尝试 {}/{}): {}", attempt + 1, maxRetries + 1, e.getMessage());
                
                if (attempt < maxRetries) {
                    logger.info("等待 {}ms 后重试...", retryDelayMs);
                    callback.onRetry(attempt + 1, e);
                    
                    try {
                        Thread.sleep(retryDelayMs);
                        handleStreamingResponseWithRetry(apiUrl, requestBody, callback, timeoutMs, attempt + 1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        callback.onError(new RuntimeException("重试被中断", ie));
                    }
                } else {
                    logger.error("达到最大重试次数，放弃请求");
                    callback.onError(new RuntimeException("流式请求失败，已达到最大重试次数", e));
                }
            }
        }, executorService);
    }
    
    public void shutdown() {
        logger.info("关闭流式响应处理器");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
