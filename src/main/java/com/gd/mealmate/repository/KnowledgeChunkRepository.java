package com.gd.mealmate.repository;

import com.gd.mealmate.model.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Query(value = "SELECT id, document_id, knowledge_base_id, chunk_index, content, created_at " +
            "FROM knowledge_chunks WHERE document_id = :docId ORDER BY chunk_index", nativeQuery = true)
    List<Object[]> findChunkDetailsByDocumentId(@Param("docId") Long documentId);

    void deleteAllByDocumentId(Long documentId);

    void deleteAllByKnowledgeBaseId(Long knowledgeBaseId);

    @Query(value = "SELECT * FROM knowledge_chunks " +
            "WHERE knowledge_base_id IN :kbIds " +
            "ORDER BY embedding <=> :queryVector::vector " +
            "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> findSimilarChunks(@Param("kbIds") List<Long> kbIds,
                                           @Param("queryVector") String queryVector,
                                           @Param("limit") int limit);
}
