package com.gd.mealmate.service;

import com.gd.mealmate.dto.request.CreateDocumentRequest;
import com.gd.mealmate.dto.request.CreateKnowledgeBaseRequest;
import com.gd.mealmate.dto.response.KnowledgeBaseDetailDto;
import com.gd.mealmate.dto.response.KnowledgeBaseDto;
import com.gd.mealmate.dto.response.KnowledgeChunkDto;
import com.gd.mealmate.dto.response.KnowledgeDocumentDto;
import com.gd.mealmate.exception.BusinessException;
import com.gd.mealmate.exception.ErrorCode;
import com.gd.mealmate.mapper.KnowledgeBaseMapper;
import com.gd.mealmate.mapper.KnowledgeDocumentMapper;
import com.gd.mealmate.model.entity.KnowledgeBase;
import com.gd.mealmate.model.entity.KnowledgeChunk;
import com.gd.mealmate.model.entity.KnowledgeDocument;
import com.gd.mealmate.model.entity.User;
import com.gd.mealmate.model.enums.DocumentContentType;
import com.gd.mealmate.repository.KnowledgeBaseRepository;
import com.gd.mealmate.repository.KnowledgeChunkRepository;
import com.gd.mealmate.repository.KnowledgeDocumentRepository;
import com.gd.mealmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final EmbeddingService embeddingService;
    private final DocumentParseService documentParseService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public KnowledgeBaseDto createKnowledgeBase(Long userId, CreateKnowledgeBaseRequest request) {
        User user = getUser(userId);
        KnowledgeBase kb = KnowledgeBase.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(user)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();
        kb = knowledgeBaseRepository.save(kb);
        KnowledgeBaseDto dto = knowledgeBaseMapper.toDto(kb);
        dto.setDocumentCount(0);
        return dto;
    }

    public Page<KnowledgeBaseDto> getKnowledgeBases(Long userId, String keyword, Pageable pageable) {
        Page<KnowledgeBase> page;
        if (keyword != null && !keyword.isBlank()) {
            page = knowledgeBaseRepository.findByUserAccessibleAndKeyword(userId, keyword, pageable);
        } else {
            page = knowledgeBaseRepository.findByUserAccessible(userId, pageable);
        }
        return page.map(kb -> {
            KnowledgeBaseDto dto = knowledgeBaseMapper.toDto(kb);
            dto.setDocumentCount(documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(kb.getId()).size());
            return dto;
        });
    }

    public KnowledgeBaseDetailDto getKnowledgeBaseDetail(Long userId, Long kbId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkReadAccess(kb, userId);

        List<KnowledgeDocument> docs = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(kbId);
        List<KnowledgeDocumentDto> docDtos = docs.stream()
                .map(knowledgeDocumentMapper::toDto)
                .toList();

        KnowledgeBaseDetailDto detail = new KnowledgeBaseDetailDto();
        detail.setId(kb.getId());
        detail.setTitle(kb.getTitle());
        detail.setDescription(kb.getDescription());
        detail.setUserId(kb.getUser().getId());
        detail.setUsername(kb.getUser().getUsername());
        detail.setIsPublic(kb.getIsPublic());
        detail.setDocuments(docDtos);
        detail.setCreatedAt(kb.getCreatedAt());
        detail.setUpdatedAt(kb.getUpdatedAt());
        return detail;
    }

    @Transactional
    public void deleteKnowledgeBase(Long userId, Long kbId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkWriteAccess(kb, userId);

        List<KnowledgeDocument> docs = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(kbId);
        for (KnowledgeDocument doc : docs) {
            deleteLocalFile(doc.getFilePath());
        }

        chunkRepository.deleteAllByKnowledgeBaseId(kbId);
        documentRepository.deleteAllByKnowledgeBaseId(kbId);
        knowledgeBaseRepository.delete(kb);
    }

    @Transactional
    public KnowledgeDocumentDto addTextDocument(Long userId, Long kbId, CreateDocumentRequest request) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkWriteAccess(kb, userId);

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .knowledgeBase(kb)
                .title(request.getTitle())
                .contentType(DocumentContentType.TEXT)
                .build();
        doc = documentRepository.save(doc);

        createAndSaveChunks(doc, kb, request.getContent());

        return knowledgeDocumentMapper.toDto(doc);
    }

    @Transactional
    public KnowledgeDocumentDto uploadFileDocument(Long userId, Long kbId, String title, MultipartFile file) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkWriteAccess(kb, userId);

        DocumentContentType contentType = resolveContentType(file.getOriginalFilename());

        Path dirPath = Path.of(uploadDir, "knowledge", String.valueOf(kbId));
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_PARSE_ERROR);
        }

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .knowledgeBase(kb)
                .title(title)
                .contentType(contentType)
                .build();
        doc = documentRepository.save(doc);

        String extension = getExtension(file.getOriginalFilename());
        Path filePath = dirPath.resolve(doc.getId() + extension);
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_PARSE_ERROR);
        }

        doc.setFilePath(filePath.toString());
        documentRepository.save(doc);

        String text = documentParseService.parseFile(filePath, contentType);
        createAndSaveChunks(doc, kb, text);

        return knowledgeDocumentMapper.toDto(doc);
    }

    @Transactional
    public void deleteDocument(Long userId, Long kbId, Long docId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkWriteAccess(kb, userId);

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));

        if (!doc.getKnowledgeBase().getId().equals(kbId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        deleteLocalFile(doc.getFilePath());
        chunkRepository.deleteAllByDocumentId(docId);
        documentRepository.delete(doc);
    }

    public List<KnowledgeChunkDto> getDocumentChunks(Long userId, Long kbId, Long docId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        checkReadAccess(kb, userId);

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));
        if (!doc.getKnowledgeBase().getId().equals(kbId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        return chunkRepository.findChunkDetailsByDocumentId(docId).stream()
                .map(row -> new KnowledgeChunkDto(
                        ((Number) row[0]).longValue(),
                        docId,
                        (Integer) row[3],
                        (String) row[4],
                        ((java.sql.Timestamp) row[5]).toLocalDateTime()))
                .toList();
    }

    public List<KnowledgeChunk> searchSimilarChunks(Long userId, String queryText, int limit) {
        Page<KnowledgeBase> publicKbs = knowledgeBaseRepository.findByUserAccessible(userId,
                Pageable.ofSize(1000));
        List<Long> kbIds = publicKbs.stream().map(KnowledgeBase::getId).distinct().toList();

        if (kbIds.isEmpty()) {
            return List.of();
        }

        float[] queryEmbedding = embeddingService.embed(queryText);
        String vectorStr = EmbeddingService.toVectorString(queryEmbedding);

        return chunkRepository.findSimilarChunks(kbIds, vectorStr, limit);
    }

    private void createAndSaveChunks(KnowledgeDocument doc, KnowledgeBase kb, String text) {
        log.info("Starting chunking: text length={}", text.length());
        List<String> chunks = documentParseService.splitIntoChunks(text);
        log.info("Split into {} chunks, sizes: {}", chunks.size(),
                chunks.stream().map(String::length).toList());
        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = embeddingService.embed(chunks.get(i));
            chunkRepository.insertChunk(
                    doc.getId(), kb.getId(), i,
                    chunks.get(i), EmbeddingService.toVectorString(embedding));
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    }

    private KnowledgeBase getKnowledgeBase(Long kbId) {
        return knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    private void checkReadAccess(KnowledgeBase kb, Long userId) {
        if (!kb.getUser().getId().equals(userId) && !Boolean.TRUE.equals(kb.getIsPublic())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_ACCESS_DENIED);
        }
    }

    private void checkWriteAccess(KnowledgeBase kb, Long userId) {
        if (!kb.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_ACCESS_DENIED);
        }
    }

    private DocumentContentType resolveContentType(String filename) {
        if (filename == null) throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return DocumentContentType.PDF;
        if (lower.endsWith(".docx")) return DocumentContentType.DOCX;
        if (lower.endsWith(".txt")) return DocumentContentType.TXT;
        throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".txt";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".txt";
    }

    private void deleteLocalFile(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", filePath, e);
            }
        }
    }
}
