package com.ai.knowledge.stream;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AdvancedStreamingHandler {
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService timeoutExecutor;
    private final Map<String, StreamingTask> activeTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final int maxConcurrentTasks;
    private final long taskTimeoutMs;
    
    public interface StreamingCallback {
        void onChunk(String chunk);
        void onComplete(String fullResponse);
        void onError(Throwable error);
        void onProgress(int currentBytes, int totalBytes);
    }
    
    private static class StreamingTask {
        final String taskId;
        final CompletableFuture<Void> future;
        final long startTime;
        final ScheduledFuture<?> timeoutFuture;
        volatile boolean completed;
        
        StreamingTask(String taskId, CompletableFuture<Void> future, ScheduledFuture<?> timeoutFuture) {
            this.taskId = taskId;
            this.future = future;
            this.startTime = System.currentTimeMillis();
            this.timeoutFuture = timeoutFuture;
            this.completed = false;
        }
        
        long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
        
        void cancel() {
            completed = true;
            future.cancel(true);
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
    }
    
    public AdvancedStreamingHandler(int maxConcurrentTasks, long taskTimeoutMs) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.taskTimeoutMs = taskTimeoutMs;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentTasks);
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        System.out.println("高级流式处理器初始化完成，最大并发任务数: " + maxConcurrentTasks + ", 任务超时: " + taskTimeoutMs + "ms");
    }
    
    public AdvancedStreamingHandler() {
        this(10, 120000);
    }
    
    public String handleStreamingResponse(
            String apiUrl,
            Map<String, Object> requestBody,
            StreamingCallback callback,
            Long timeoutMs
    ) {
        if (activeTasks.size() >= maxConcurrentTasks) {
            callback.onError(new RuntimeException("已达到最大并发任务数限制: " + maxConcurrentTasks));
            return null;
        }
        
        String taskId = "task-" + taskCounter.incrementAndGet() + "-" + System.currentTimeMillis();
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("开始处理流式任务: " + taskId);
                
                ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() -> {
                    StreamingTask task = activeTasks.get(taskId);
                    if (task != null && !task.completed) {
                        System.err.println("任务超时: " + taskId + ", 持续时间: " + task.getDuration() + "ms");
                        task.cancel();
                        callback.onError(new TimeoutException("任务超时: " + taskId));
                    }
                }, timeoutMs != null ? timeoutMs : taskTimeoutMs, TimeUnit.MILLISECONDS);
                
                boolean completed = false;
                
                cn.hutool.http.HttpResponse httpResponse = cn.hutool.http.HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer 8a909ec7d9344b6ea0d89701111789cc.Mr0DIPfbCsZNXALP")
                    .header("Content-Type", "application/json")
                    .body(new cn.hutool.json.JSONObject(requestBody).toString())
                    .timeout((int) (timeoutMs != null ? timeoutMs : 300000))
                    .setConnectionTimeout(30000)
                    .execute();
                
                System.out.println("AI API响应状态: " + httpResponse.getStatus() + ", 任务: " + taskId);
                
                if (httpResponse.getStatus() != 200) {
                    throw new RuntimeException("AI API返回错误状态: " + httpResponse.getStatus());
                }
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(httpResponse.bodyStream())
                );
                String line;
                StringBuilder fullResponse = new StringBuilder();
                int totalBytes = 0;
                
                while ((line = reader.readLine()) != null && !completed) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if (data.equals("[DONE]")) {
                            System.out.println("流式响应完成，任务: " + taskId);
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
                                    totalBytes += content.getBytes().length;
                                    System.out.println("发送流式数据: " + content + ", 任务: " + taskId);
                                    callback.onChunk(content);
                                    callback.onProgress(totalBytes, -1);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("解析流式数据失败: " + e.getMessage() + ", 任务: " + taskId);
                        }
                    }
                }
                
                reader.close();
                timeoutFuture.cancel(false);
                
                String finalResponse = fullResponse.toString();
                System.out.println("流式响应最终完成，任务: " + taskId + ", 总长度: " + finalResponse.length() + ", 耗时: " + 
                    activeTasks.get(taskId).getDuration() + "ms");
                completed = true;
                callback.onProgress(totalBytes, totalBytes);
                callback.onComplete(finalResponse);
                
            } catch (Exception e) {
                System.err.println("流式任务异常: " + taskId + ", 错误: " + e.getMessage());
                callback.onError(e);
            } finally {
                completeTask(taskId);
            }
        }, executorService);
        
        StreamingTask task = new StreamingTask(taskId, future, null);
        activeTasks.put(taskId, task);
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                System.err.println("任务异常完成: " + taskId + ", 错误: " + error.getMessage());
            }
        });
        
        System.out.println("创建流式任务: " + taskId + ", 当前活跃任务数: " + activeTasks.size());
        return taskId;
    }
    
    private void completeTask(String taskId) {
        StreamingTask task = activeTasks.remove(taskId);
        if (task != null) {
            task.completed = true;
            System.out.println("任务完成: " + taskId + ", 耗时: " + task.getDuration() + "ms, 剩余任务: " + activeTasks.size());
        }
    }
    
    public boolean cancelTask(String taskId) {
        StreamingTask task = activeTasks.get(taskId);
        if (task != null && !task.completed) {
            task.cancel();
            completeTask(taskId);
            return true;
        }
        return false;
    }
    
    public void shutdown() {
        System.out.println("关闭高级流式处理器，取消 " + activeTasks.size() + " 个活跃任务");
        
        for (StreamingTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        
        executorService.shutdown();
        timeoutExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("高级流式处理器已关闭");
    }
    
    public Map<String, Object> getStats() {
        return Map.of(
            "activeTasks", activeTasks.size(),
            "maxConcurrentTasks", maxConcurrentTasks,
            "totalTasksCreated", taskCounter.get(),
            "taskTimeoutMs", taskTimeoutMs
        );
    }
    
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
    
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
}
