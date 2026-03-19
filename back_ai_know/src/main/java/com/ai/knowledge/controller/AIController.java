package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.dto.ChatAutoRequest;
import com.ai.knowledge.dto.ChatRequest;
import com.ai.knowledge.dto.GenerateRequest;
import com.ai.knowledge.dto.ParseRequest;
import com.ai.knowledge.entity.GraphEdge;
import com.ai.knowledge.entity.GraphNode;
import com.ai.knowledge.entity.KnowledgeGraph;
import com.ai.knowledge.service.AIService;
import com.ai.knowledge.service.KnowledgeGraphService;
import com.ai.knowledge.stream.AdvancedStreamingHandler;
import com.ai.knowledge.stream.StreamingResponseHandler;
import com.ai.knowledge.stream.StreamingHandlerManager;
import com.ai.knowledge.stream.StreamingResponseHandlerWithRetry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI对话管理")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;
    
    @PostMapping("/generate")
    @Operation(summary = "生成知识图谱")
    public Result<String> generateGraph(@RequestBody GenerateRequest request) {
        String result = aiService.generateKnowledgeGraph(request.getText(), request.getIsPublic());
        return Result.success(result);
    }
    
    @PostMapping("/parse")
    @Operation(summary = "解析知识图谱")
    public Result<List<Map<String, Object>>> parseGraph(@RequestBody ParseRequest request) {
        List<Map<String, Object>> result = aiService.parseKnowledgeGraph(request.getText());
        return Result.success(result);
    }
    
    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.getMessage(), request.getHistory());
        return Result.success(response);
    }
    
    @PostMapping("/chat/auto")
    @Operation(summary = "AI对话（自动保存历史）")
    public Result<String> chatWithAutoSave(@RequestBody ChatAutoRequest request) {
        String response = aiService.chatWithAutoSave(request.getUserId(), request.getMessage());
        return Result.success(response);
    }
    
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI对话（流式响应）")
    public SseEmitter chatStream(@RequestBody ChatAutoRequest request) {
        System.out.println("收到流式请求: userId=" + request.getUserId() + ", message=" + request.getMessage());
        
        boolean isKnowledgeGraph = request.getMessage().contains("知识图谱");
        System.out.println("是否为知识图谱请求: " + isKnowledgeGraph);
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> {
            System.out.println("用户 " + request.getUserId() + " 的流式连接完成");
        });
        
        emitter.onTimeout(() -> {
            System.out.println("用户 " + request.getUserId() + " 的流式连接超时");
        });
        
        StreamingResponseHandler streamingHandler = new StreamingResponseHandler();
        
        streamingHandler.handleStreamingResponse(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            buildChatRequest(request),
            new StreamingResponseHandler.StreamingCallback() {
                private final StringBuilder responseBuilder = new StringBuilder();
                
                @Override
                public void onChunk(String chunk) {
                    try {
                        responseBuilder.append(chunk);
                        emitter.send(SseEmitter.event().data(chunk));
                        System.out.println("发送流式数据: " + chunk);
                    } catch (Exception e) {
                        System.err.println("发送流式数据失败: " + e.getMessage());
                    }
                }
                
                @Override
                public String onComplete(String fullResponse) {
                    String finalResponse = fullResponse;
                    try {
                        System.out.println("流式响应完成，总长度: " + fullResponse.length());
                        if (isKnowledgeGraph) {
                            System.out.println("检测到知识图谱请求，开始自动创建");
                            Long graphId = createKnowledgeGraphFromAI(fullResponse, request.getUserId(), request.getMessage());
                            if (graphId != null) {
                                finalResponse = "知识图谱创建成功！图谱ID: " + graphId;
                            } else {
                                finalResponse = "知识图谱创建失败，请稍后重试。";
                            }
                        }
                        
                        List<Map<String, Object>> history = aiService.getChatHistory(request.getUserId());
                        history.add(Map.of("role", "user", "content", request.getMessage()));
                        history.add(Map.of("role", "assistant", "content", finalResponse));
                        
                        System.out.println("准备保存Redis历史记录，用户ID: " + request.getUserId() + ", 历史记录数量: " + history.size());
                        aiService.saveChatHistory(request.getUserId(), history);
                        System.out.println("Redis历史记录保存成功");
                        
                        emitter.complete();
                        System.out.println("SseEmitter已完成，不再发送数据");
                    } catch (Exception e) {
                        System.err.println("完成流式响应失败: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            System.err.println("完成错误发送失败: " + ex.getMessage());
                        }
                    }
                    return finalResponse;
                }
                
                @Override
                public void onError(Throwable error) {
                    System.err.println("流式响应错误: " + error.getMessage());
                    error.printStackTrace();
                    try {
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        System.err.println("发送错误失败: " + e.getMessage());
                    }
                }
            },
            120000L, isKnowledgeGraph
        );
        
        return emitter;
    }
    
    private Map<String, Object> buildChatRequest(ChatAutoRequest request) {
        List<Map<String, Object>> history = aiService.getChatHistory(request.getUserId());
        List<Map<String, Object>> messages = new ArrayList<>();
        
        String systemPrompt = request.getMessage().contains("知识图谱") ? 
            "你是一个知识图谱生成助手。请分析用户提供的文本，提取其中的实体（节点）和关系（边），并以JSON格式返回。" +
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
            "6. 返回的内容必须以{开始，以}结束，中间不能有其他内容" : 
            "你是一个智能助手，可以帮助用户解答问题、提供建议和进行日常对话。请用自然、友好的语气回答用户的问题。";
        
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));
        
        return Map.of(
            "model", "glm-4.6v",
            "messages", messages,
            "max_tokens", 2000,
            "temperature", 0.7,
            "stream", true
        );
    }
    
    @PostMapping(value = "/chat/non-stream")
    @Operation(summary = "AI对话（非流式响应）")
    public Result<String> chatNonStream(@RequestBody ChatAutoRequest request) {
        System.out.println("收到非流式请求: userId=" + request.getUserId() + ", message=" + request.getMessage());
        
        try {
            String response = aiService.chatNonStream(request.getUserId(), request.getMessage());
            
            List<Map<String, Object>> history = aiService.getChatHistory(request.getUserId());
            history.add(Map.of("role", "user", "content", request.getMessage()));
            history.add(Map.of("role", "assistant", "content", response));
            aiService.saveChatHistory(request.getUserId(), history);
            
            return Result.success(response);
        } catch (Exception e) {
            System.err.println("非流式请求失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("AI服务暂时不可用，请稍后重试。错误信息：" + e.getMessage());
        }
    }
    
    @GetMapping("/history/{userId}")
    @Operation(summary = "获取对话历史")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable Long userId) {
        List<Map<String, Object>> history = aiService.getChatHistory(userId);
        return Result.success(history);
    }
    
    @DeleteMapping("/history/{userId}")
    @Operation(summary = "清除对话历史")
    public Result<Void> clearHistory(@PathVariable Long userId) {
        aiService.clearChatHistory(userId);
        return Result.success(null);
    }
    
    @PostMapping(value = "/chat/stream/retry", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI对话（带重试的流式响应）")
    public SseEmitter chatStreamWithRetry(@RequestBody ChatAutoRequest request) {
        System.out.println("收到带重试的流式请求: userId=" + request.getUserId() + ", message=" + request.getMessage());
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> {
            System.out.println("用户 " + request.getUserId() + " 的带重试流式连接完成");
        });
        
        emitter.onTimeout(() -> {
            System.out.println("用户 " + request.getUserId() + " 的带重试流式连接超时");
        });
        
        StreamingResponseHandlerWithRetry streamingHandler = new StreamingResponseHandlerWithRetry(3, 2000);
        
        streamingHandler.handleStreamingResponseWithRetry(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            buildChatRequest(request),
            new StreamingResponseHandlerWithRetry.StreamingCallback() {
                private final StringBuilder responseBuilder = new StringBuilder();
                
                @Override
                public void onChunk(String chunk) {
                    try {
                        responseBuilder.append(chunk);
                        emitter.send(SseEmitter.event().data(chunk));
                        System.out.println("发送带重试流式数据: " + chunk);
                    } catch (Exception e) {
                        System.err.println("发送带重试流式数据失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onComplete(String fullResponse) {
                    try {
                        System.out.println("带重试流式响应完成，总长度: " + fullResponse.length());
                        
                        List<Map<String, Object>> history = aiService.getChatHistory(request.getUserId());
                        history.add(Map.of("role", "user", "content", request.getMessage()));
                        history.add(Map.of("role", "assistant", "content", fullResponse));
                        
                        System.out.println("准备保存Redis历史记录，用户ID: " + request.getUserId() + ", 历史记录数量: " + history.size());
                        aiService.saveChatHistory(request.getUserId(), history);
                        System.out.println("Redis历史记录保存成功");
                        
                        emitter.complete();
                        System.out.println("SseEmitter已完成，不再发送数据");
                    } catch (Exception e) {
                        System.err.println("完成带重试流式响应失败: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            System.err.println("完成错误发送失败: " + ex.getMessage());
                        }
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    System.err.println("带重试流式响应错误: " + error.getMessage());
                    error.printStackTrace();
                    try {
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        System.err.println("发送带重试错误失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onRetry(int attempt, Throwable lastError) {
                    try {
                        emitter.send(SseEmitter.event().data("重试中... (" + attempt + "/" + streamingHandler.getMaxRetries() + ")"));
                        System.out.println("正在重试第 " + attempt + " 次，上次错误: " + lastError.getMessage());
                    } catch (Exception e) {
                        System.err.println("发送重试信息失败: " + e.getMessage());
                    }
                }
            },
            300000L
        );
        
        return emitter;
    }
    
    @PostMapping(value = "/chat/stream/advanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI对话（高级流式响应）")
    public SseEmitter chatStreamAdvanced(@RequestBody ChatAutoRequest request) {
        System.out.println("收到高级流式请求: userId=" + request.getUserId() + ", message=" + request.getMessage());
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> {
            System.out.println("用户 " + request.getUserId() + " 的高级连接完成");
        });
        
        emitter.onTimeout(() -> {
            System.out.println("用户 " + request.getUserId() + " 的高级连接超时");
        });
        
        AdvancedStreamingHandler streamingHandler = StreamingHandlerManager.getInstance().getDefaultHandler();
        
        String taskId = streamingHandler.handleStreamingResponse(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            buildChatRequest(request),
            new AdvancedStreamingHandler.StreamingCallback() {
                private final StringBuilder responseBuilder = new StringBuilder();
                
                @Override
                public void onChunk(String chunk) {
                    try {
                        responseBuilder.append(chunk);
                        emitter.send(SseEmitter.event().data(chunk));
                        System.out.println("发送高级流式数据: " + chunk);
                    } catch (Exception e) {
                        System.err.println("发送高级流式数据失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onComplete(String fullResponse) {
                    try {
                        System.out.println("高级流式响应完成，总长度: " + fullResponse.length());
                        
                        List<Map<String, Object>> history = aiService.getChatHistory(request.getUserId());
                        history.add(Map.of("role", "user", "content", request.getMessage()));
                        history.add(Map.of("role", "assistant", "content", fullResponse));
                        
                        System.out.println("准备保存Redis历史记录，用户ID: " + request.getUserId() + ", 历史记录数量: " + history.size());
                        aiService.saveChatHistory(request.getUserId(), history);
                        System.out.println("Redis历史记录保存成功");
                        
                        emitter.complete();
                        System.out.println("SseEmitter已完成，不再发送数据");
                    } catch (Exception e) {
                        System.err.println("完成高级流式响应失败: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            System.err.println("完成错误发送失败: " + ex.getMessage());
                        }
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    System.err.println("高级流式响应错误: " + error.getMessage());
                    error.printStackTrace();
                    try {
                        emitter.send(SseEmitter.event().data("错误: " + error.getMessage()));
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        System.err.println("发送错误失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onProgress(int currentBytes, int totalBytes) {
                    System.out.println("高级流式响应进度: " + currentBytes + " 字节");
                }
            },
            300000L
        );
        
        System.out.println("创建高级流式任务: " + taskId);
        return emitter;
    }
    
    @GetMapping("/stream/stats")
    @Operation(summary = "获取流式处理器统计信息")
    public Map<String, Object> getStreamStats() {
        return StreamingHandlerManager.getInstance().getAllStats();
    }
    
    private Long createKnowledgeGraphFromAI(String aiResponse, Long userId, String userMessage) {
        try {
            System.out.println("开始从AI响应创建知识图谱");
            
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
            Matcher matcher = pattern.matcher(aiResponse);
            
            if (!matcher.find()) {
                System.out.println("未找到JSON格式的图谱数据");
                return null;
            }
            
            String jsonStr = matcher.group();
            System.out.println("提取的JSON: " + jsonStr.substring(0, Math.min(200, jsonStr.length())) + "...");
            
            cn.hutool.json.JSONObject graphJson = cn.hutool.json.JSONUtil.parseObj(jsonStr);
            
            if (!graphJson.containsKey("nodes") || !graphJson.containsKey("edges")) {
                System.out.println("JSON数据格式不正确，缺少nodes或edges");
                return null;
            }
            
            List<cn.hutool.json.JSONObject> nodesJson = graphJson.getJSONArray("nodes").toList(cn.hutool.json.JSONObject.class);
            List<cn.hutool.json.JSONObject> edgesJson = graphJson.getJSONArray("edges").toList(cn.hutool.json.JSONObject.class);
            
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            
            for (cn.hutool.json.JSONObject nodeObj : nodesJson) {
                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("id", nodeObj.getStr("id"));
                nodeMap.put("name", nodeObj.getStr("name"));
                nodeMap.put("category", nodeObj.getStr("category"));
                nodes.add(nodeMap);
            }
            
            for (cn.hutool.json.JSONObject edgeObj : edgesJson) {
                Map<String, Object> edgeMap = new HashMap<>();
                edgeMap.put("source", edgeObj.getStr("source"));
                edgeMap.put("target", edgeObj.getStr("target"));
                edgeMap.put("label", edgeObj.getStr("label"));
                edges.add(edgeMap);
            }
            
            System.out.println("节点数量: " + nodes.size() + ", 边数量: " + edges.size());
            
            String graphName = userMessage.replace("创建.*?知识图谱.*?关于", "").trim();
            if (graphName.isEmpty()) {
                graphName = "AI生成的知识图谱";
            }
            
            KnowledgeGraph graph = new KnowledgeGraph();
            graph.setName(graphName);
            graph.setDescription("由AI自动生成的知识图谱，基于用户请求：" + userMessage);
            graph.setCategoryId(1L);
            graph.setOwnerId(userId);
            graph.setIsPublic(0);
            
            boolean graphCreated = knowledgeGraphService.createGraph(graph);
            if (!graphCreated) {
                System.out.println("创建知识图谱失败");
                return null;
            }
            
            System.out.println("知识图谱创建成功，ID: " + graph.getId());
            
            for (Map<String, Object> nodeData : nodes) {
                GraphNode node = new GraphNode();
                node.setGraphId(graph.getId());
                node.setNodeId((String) nodeData.get("id"));
                node.setName((String) nodeData.get("name"));
                node.setCategory((String) nodeData.get("category"));
                
                boolean nodeCreated = knowledgeGraphService.createNode(node);
                if (!nodeCreated) {
                    System.err.println("创建节点失败: " + nodeData.get("name"));
                }
            }
            
            System.out.println("所有节点创建完成");
            
            for (Map<String, Object> edgeData : edges) {
                GraphEdge edge = new GraphEdge();
                edge.setGraphId(graph.getId());
                edge.setSourceNodeId((String) edgeData.get("source"));
                edge.setTargetNodeId((String) edgeData.get("target"));
                edge.setRelation((String) edgeData.get("label"));
                
                boolean edgeCreated = knowledgeGraphService.createEdge(edge);
                if (!edgeCreated) {
                    System.err.println("创建边失败: " + edgeData.get("label"));
                }
            }
            
            System.out.println("所有边创建完成，知识图谱创建完成");
            return graph.getId();
            
        } catch (Exception e) {
            System.err.println("创建知识图谱异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}