package com.ai.knowledge.controller;

import com.ai.knowledge.common.Result;
import com.ai.knowledge.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/excel")
@Tag(name = "Excel导入导出")
public class ExcelController {
    
    @Autowired
    private ExcelService excelService;
    
    @GetMapping("/export/{graphId}")
    @Operation(summary = "导出知识图谱为Excel")
    public ResponseEntity<ByteArrayResource> exportToExcel(@PathVariable Long graphId) {
        byte[] excelData = excelService.exportToExcelBytes(graphId);
        if (excelData == null) {
            return ResponseEntity.notFound().build();
        }
        
        String fileName = "知识图谱_" + graphId + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(excelData.length)
                .body(new ByteArrayResource(excelData));
    }
    
    @PostMapping("/import/nodes/{graphId}")
    @Operation(summary = "导入节点数据")
    public Result<Void> importNodes(
        @PathVariable Long graphId,
        @RequestParam("file") MultipartFile file) throws IOException {
        
        byte[] excelData = file.getBytes();
        excelService.importNodesFromExcel(graphId, excelData);
        return Result.success(null);
    }
    
    @PostMapping("/import/edges/{graphId}")
    @Operation(summary = "导入关系数据")
    public Result<Void> importEdges(
        @PathVariable Long graphId,
        @RequestParam("file") MultipartFile file) throws IOException {
        
        byte[] excelData = file.getBytes();
        excelService.importEdgesFromExcel(graphId, excelData);
        return Result.success(null);
    }
}