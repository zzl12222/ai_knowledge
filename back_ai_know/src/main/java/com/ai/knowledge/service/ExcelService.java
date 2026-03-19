package com.ai.knowledge.service;

import com.ai.knowledge.entity.GraphNode;
import com.ai.knowledge.entity.GraphEdge;

import java.util.List;

public interface ExcelService {
    
    String exportToExcel(Long graphId);
    
    byte[] exportToExcelBytes(Long graphId);
    
    List<GraphNode> importNodesFromExcel(Long graphId, byte[] excelData);
    
    List<GraphEdge> importEdgesFromExcel(Long graphId, byte[] excelData);
}