package com.ai.knowledge.stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamingHandlerManager {
    
    private static volatile StreamingHandlerManager instance;
    private final Map<String, AdvancedStreamingHandler> handlers = new ConcurrentHashMap<>();
    private final AdvancedStreamingHandler defaultHandler;
    
    private StreamingHandlerManager() {
        this.defaultHandler = new AdvancedStreamingHandler(10, 120000);
        handlers.put("default", defaultHandler);
        System.out.println("流式处理器管理器初始化完成");
    }
    
    public static StreamingHandlerManager getInstance() {
        if (instance == null) {
            synchronized (StreamingHandlerManager.class) {
                if (instance == null) {
                    instance = new StreamingHandlerManager();
                }
            }
        }
        return instance;
    }
    
    public AdvancedStreamingHandler getHandler(String handlerName) {
        return handlers.get(handlerName);
    }
    
    public AdvancedStreamingHandler getDefaultHandler() {
        return defaultHandler;
    }
    
    public AdvancedStreamingHandler createHandler(String handlerName, int maxConcurrentTasks, long taskTimeoutMs) {
        AdvancedStreamingHandler handler = new AdvancedStreamingHandler(maxConcurrentTasks, taskTimeoutMs);
        handlers.put(handlerName, handler);
        System.out.println("创建流式处理器: " + handlerName + ", 最大并发: " + maxConcurrentTasks + ", 超时: " + taskTimeoutMs + "ms");
        return handler;
    }
    
    public void removeHandler(String handlerName) {
        if (!handlerName.equals("default")) {
            AdvancedStreamingHandler handler = handlers.remove(handlerName);
            if (handler != null) {
                handler.shutdown();
                System.out.println("移除流式处理器: " + handlerName);
            }
        }
    }
    
    public Map<String, Object> getAllStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        for (Map.Entry<String, AdvancedStreamingHandler> entry : handlers.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        return stats;
    }
    
    public void shutdown() {
        System.out.println("关闭所有流式处理器");
        for (AdvancedStreamingHandler handler : handlers.values()) {
            handler.shutdown();
        }
        handlers.clear();
    }
}
