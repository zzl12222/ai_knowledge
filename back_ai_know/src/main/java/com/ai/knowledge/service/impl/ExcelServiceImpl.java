package com.ai.knowledge.service.impl;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.ai.knowledge.entity.GraphEdge;
import com.ai.knowledge.entity.GraphNode;
import com.ai.knowledge.entity.KnowledgeGraph;
import com.ai.knowledge.mapper.GraphEdgeMapper;
import com.ai.knowledge.mapper.GraphNodeMapper;
import com.ai.knowledge.mapper.KnowledgeGraphMapper;
import com.ai.knowledge.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExcelServiceImpl implements ExcelService {
    
    @Autowired
    private GraphNodeMapper graphNodeMapper;
    
    @Autowired
    private GraphEdgeMapper graphEdgeMapper;
    
    @Autowired
    private KnowledgeGraphMapper knowledgeGraphMapper;
    
    @Override
    public String exportToExcel(Long graphId) {
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        if (graph == null) {
            return null;
        }
        
        List<GraphNode> nodes = graphNodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphNode>()
                .eq(GraphNode::getGraphId, graphId)
        );
        
        List<GraphEdge> edges = graphEdgeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEdge>()
                .eq(GraphEdge::getGraphId, graphId)
        );
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ExcelWriter writer = ExcelUtil.getWriter(true);
        
        writer.setSheet("节点");
        writer.writeRow(List.of("节点ID", "节点名称", "节点类别", "属性"));
        for (GraphNode node : nodes) {
            writer.writeRow(List.of(
                node.getNodeId(),
                node.getName(),
                node.getCategory(),
                node.getProperties()
            ));
        }
        
        writer.setSheet("关系");
        writer.writeRow(List.of("起点节点ID", "终点节点ID", "关系名称", "属性"));
        for (GraphEdge edge : edges) {
            writer.writeRow(List.of(
                edge.getSourceNodeId(),
                edge.getTargetNodeId(),
                edge.getRelation(),
                edge.getProperties()
            ));
        }
        
        writer.flush(outputStream);
        writer.close();
        
        return "知识图谱_" + graph.getName() + "_导出.xlsx";
    }
    
    @Override
    public byte[] exportToExcelBytes(Long graphId) {
        KnowledgeGraph graph = knowledgeGraphMapper.selectById(graphId);
        if (graph == null) {
            return null;
        }
        
        List<GraphNode> nodes = graphNodeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphNode>()
                .eq(GraphNode::getGraphId, graphId)
        );
        
        List<GraphEdge> edges = graphEdgeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEdge>()
                .eq(GraphEdge::getGraphId, graphId)
        );
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ExcelWriter writer = ExcelUtil.getWriter(true);
        
        writer.setSheet("节点");
        writer.writeRow(List.of("节点ID", "节点名称", "节点类别", "属性"));
        for (GraphNode node : nodes) {
            writer.writeRow(List.of(
                node.getNodeId(),
                node.getName(),
                node.getCategory(),
                node.getProperties()
            ));
        }
        
        writer.setSheet("关系");
        writer.writeRow(List.of("起点节点ID", "终点节点ID", "关系名称", "属性"));
        for (GraphEdge edge : edges) {
            writer.writeRow(List.of(
                edge.getSourceNodeId(),
                edge.getTargetNodeId(),
                edge.getRelation(),
                edge.getProperties()
            ));
        }
        
        writer.flush(outputStream);
        writer.close();
        
        return outputStream.toByteArray();
    }
    
    @Override
    public List<GraphNode> importNodesFromExcel(Long graphId, byte[] excelData) {
        ExcelReader reader = ExcelUtil.getReader(new java.io.ByteArrayInputStream(excelData));
        reader.setSheet("节点");
        List<Map<String, Object>> rows = reader.readAll();
        
        List<GraphNode> nodes = new ArrayList<>();
        
        for (int i = 1; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            
            GraphNode node = new GraphNode();
            node.setGraphId(graphId);
            node.setNodeId(String.valueOf(row.get("节点ID")));
            node.setName(String.valueOf(row.get("节点名称")));
            node.setCategory(String.valueOf(row.get("节点类别")));
            node.setProperties(row.get("属性") != null ? String.valueOf(row.get("属性")) : null);
            
            nodes.add(node);
        }
        
        for (GraphNode node : nodes) {
            graphNodeMapper.insert(node);
        }
        
        return nodes;
    }
    
    @Override
    public List<GraphEdge> importEdgesFromExcel(Long graphId, byte[] excelData) {
        ExcelReader reader = ExcelUtil.getReader(new java.io.ByteArrayInputStream(excelData));
        reader.setSheet("关系");
        List<Map<String, Object>> rows = reader.readAll();
        
        List<GraphEdge> edges = new ArrayList<>();
        
        for (int i = 1; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            
            GraphEdge edge = new GraphEdge();
            edge.setGraphId(graphId);
            edge.setSourceNodeId(String.valueOf(row.get("起点节点ID")));
            edge.setTargetNodeId(String.valueOf(row.get("终点节点ID")));
            edge.setRelation(String.valueOf(row.get("关系名称")));
            edge.setProperties(row.get("属性") != null ? String.valueOf(row.get("属性")) : null);
            
            edges.add(edge);
        }
        
        for (GraphEdge edge : edges) {
            graphEdgeMapper.insert(edge);
        }
        
        return edges;
    }
}