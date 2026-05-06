package com.gd.mealmate.controller;

import com.gd.mealmate.dto.request.CreateDocumentRequest;
import com.gd.mealmate.dto.request.CreateKnowledgeBaseRequest;
import com.gd.mealmate.dto.response.ApiResponse;
import com.gd.mealmate.dto.response.KnowledgeBaseDetailDto;
import com.gd.mealmate.dto.response.KnowledgeBaseDto;
import com.gd.mealmate.dto.response.KnowledgeChunkDto;
import com.gd.mealmate.dto.response.KnowledgeDocumentDto;
import com.gd.mealmate.security.UserPrincipal;
import com.gd.mealmate.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "Knowledge Bases", description = "Knowledge base management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    @Operation(summary = "Create knowledge base")
    public ApiResponse<KnowledgeBaseDto> createKnowledgeBase(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.createKnowledgeBase(principal.getUserId(), request));
    }

    @GetMapping
    @Operation(summary = "Get all knowledge bases")
    public ApiResponse<Page<KnowledgeBaseDto>> getKnowledgeBases(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(knowledgeBaseService.getKnowledgeBases(principal.getUserId(), keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get knowledge base detail")
    public ApiResponse<KnowledgeBaseDetailDto> getKnowledgeBaseDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long id) {
        return ApiResponse.success(knowledgeBaseService.getKnowledgeBaseDetail(principal.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete knowledge base")
    public ApiResponse<Void> deleteKnowledgeBase(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(principal.getUserId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/documents/text")
    @Operation(summary = "Add text document to knowledge base")
    public ApiResponse<KnowledgeDocumentDto> addTextDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long id,
            @Valid @RequestBody CreateDocumentRequest request) {
        return ApiResponse.success(knowledgeBaseService.addTextDocument(principal.getUserId(), id, request));
    }

    @PostMapping("/{id}/documents/file")
    @Operation(summary = "Upload file document to knowledge base")
    public ApiResponse<KnowledgeDocumentDto> uploadFileDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long id,
            @Parameter(description = "Document title") @RequestParam String title,
            @Parameter(description = "File (PDF/DOCX/TXT)") @RequestParam MultipartFile file) {
        return ApiResponse.success(knowledgeBaseService.uploadFileDocument(principal.getUserId(), id, title, file));
    }

    @DeleteMapping("/{kbId}/documents/{docId}")
    @Operation(summary = "Delete document from knowledge base")
    public ApiResponse<Void> deleteDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long kbId,
            @Parameter(description = "Document ID") @PathVariable Long docId) {
        knowledgeBaseService.deleteDocument(principal.getUserId(), kbId, docId);
        return ApiResponse.success();
    }

    @GetMapping("/{kbId}/documents/{docId}/chunks")
    @Operation(summary = "Get document chunks")
    public ApiResponse<List<KnowledgeChunkDto>> getDocumentChunks(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Knowledge base ID") @PathVariable Long kbId,
            @Parameter(description = "Document ID") @PathVariable Long docId) {
        return ApiResponse.success(knowledgeBaseService.getDocumentChunks(principal.getUserId(), kbId, docId));
    }
}
