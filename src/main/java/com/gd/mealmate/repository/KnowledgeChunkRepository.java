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

    void deleteAllByDocumentId(Long documentId);

    void deleteAllByKnowledgeBaseId(Long knowledgeBaseId);

    @Query(value = "SELECT * FROM knowledge_chunks " +
            "WHERE knowledge_base_id IN :kbIds " +
            "ORDER BY embedding <=> to_vector(:queryVector) " +
            "LIMIT :limit", nativeQuery = true)
    List<KnowledgeChunk> findSimilarChunks(@Param("kbIds") List<Long> kbIds,
                                           @Param("queryVector") String queryVector,
                                           @Param("limit") int limit);
}
