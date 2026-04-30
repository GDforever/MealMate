# Knowledge Base with RAG - Design Spec

## Overview

Add a nutritionist knowledge base module to MealMate with RAG (Retrieval-Augmented Generation) capabilities. Users can create knowledge bases, upload documents (PDF/DOCX/TXT) or input text, and the AI chat will leverage this knowledge for better responses.

## Requirements

1. Get all knowledge bases (list with pagination and keyword search)
2. Get knowledge base details (with document list)
3. Delete knowledge base (cascade delete documents and files)
4. Upload knowledge base (create + add text/file documents)
5. AI chat integration via RAG retrieval

## Data Model

### KnowledgeBase Entity

| Field       | Type              | Description            |
|-------------|-------------------|------------------------|
| id          | Long              | Primary key            |
| title       | String(255)       | Title                  |
| description | String(1000)      | Description            |
| user        | User (FK)         | Creator                |
| isPublic    | Boolean           | Whether publicly visible |
| createdAt   | LocalDateTime     | Created at             |
| updatedAt   | LocalDateTime     | Updated at             |

### KnowledgeDocument Entity

| Field         | Type                | Description                     |
|---------------|---------------------|---------------------------------|
| id            | Long                | Primary key                     |
| knowledgeBase | KnowledgeBase (FK)  | Parent knowledge base           |
| title         | String(255)         | Document title                  |
| contentType   | Enum (TEXT/PDF/DOCX/TXT) | Source type                |
| filePath      | String              | Original file path (if uploaded)|
| createdAt     | LocalDateTime       | Created at                      |

### KnowledgeChunk Entity (Vector Store)

| Field          | Type                | Description                     |
|----------------|---------------------|---------------------------------|
| id             | Long                | Primary key                     |
| document       | KnowledgeDocument (FK) | Parent document              |
| knowledgeBase  | KnowledgeBase (FK)  | For direct KB-level queries      |
| chunkIndex     | Integer             | Chunk sequence number           |
| content        | String(text)        | Chunk text content              |
| embedding      | float[1536]         | Vector embedding (PGVector)     |
| createdAt      | LocalDateTime       | Created at                      |

### ER Relationships

```
User 1 ──── N KnowledgeBase 1 ──── N KnowledgeDocument 1 ──── N KnowledgeChunk
```

## API Design

### Create Knowledge Base

```
POST /api/knowledge-bases
Authorization: Bearer <token>
Body: { "title": "...", "description": "...", "isPublic": false }

Response: ApiResponse<KnowledgeBaseDto>
```

### Get All Knowledge Bases

```
GET /api/knowledge-bases?page=0&size=10&keyword=营养
Authorization: Bearer <token>

Response: ApiResponse<Page<KnowledgeBaseDto>>
- Normal users: own + public knowledge bases
- Admins: all knowledge bases
```

### Get Knowledge Base Detail

```
GET /api/knowledge-bases/{id}
Authorization: Bearer <token>

Response: ApiResponse<KnowledgeBaseDetailDto>
- Returns KB info + document list
- Accessible if owner or KB is public
```

### Delete Knowledge Base

```
DELETE /api/knowledge-bases/{id}
Authorization: Bearer <token>

Response: ApiResponse<Void>
- Cascades: deletes documents, chunks (vectors), and local files
- Owner or admin only
```

### Add Text Document

```
POST /api/knowledge-bases/{id}/documents/text
Authorization: Bearer <token>
Body: { "title": "...", "content": "..." }

Response: ApiResponse<KnowledgeDocumentDto>
- Splits text into chunks (~500 tokens each)
- Generates embeddings for each chunk
```

### Upload File Document

```
POST /api/knowledge-bases/{id}/documents/file
Authorization: Bearer <token>
Content-Type: multipart/form-data
File: PDF/DOCX/TXT file

Response: ApiResponse<KnowledgeDocumentDto>
- Parses file to extract text
- Splits into chunks, generates embeddings
```

### Delete Document

```
DELETE /api/knowledge-bases/{kbId}/documents/{docId}
Authorization: Bearer <token>

Response: ApiResponse<Void>
- Deletes document, its chunks, and local file
```

## RAG Integration

### Document Ingestion Pipeline

```
Upload/File → Parse → Extract Text → Split into Chunks (~500 tokens)
→ Embedding API → Store in PGVector (KnowledgeChunk)
```

### Retrieval Pipeline

```
User Question → Embed Query → PGVector Similarity Search (Top-K=5)
→ Inject matched chunks into System Prompt → AI generates response
```

### ChatService Integration

Two integration points in existing ChatService:

1. **Passive injection**: In `buildSystemPrompt()`, retrieve relevant knowledge chunks based on the user message and append to the system prompt.

2. **Active retrieval (AI Function)**: Add `SearchKnowledgeBase` function to the existing AI function calling setup. The AI decides when to query the knowledge base.

Initial implementation will use passive injection. The function calling approach can be added as a follow-up.

### Similarity Search

```sql
SELECT * FROM knowledge_chunks
WHERE knowledge_base_id IN (:accessible_kb_ids)
ORDER BY embedding <=> :query_embedding
LIMIT :top_k;
```

Uses cosine distance (`<=>`) provided by PGVector.

## File Processing

### Supported Formats

| Format | Library   | Approach                |
|--------|-----------|-------------------------|
| TXT    | Built-in  | Direct read             |
| PDF    | PDFBox    | Text extraction per page|
| DOCX   | Apache POI| Paragraph extraction    |

### Storage

- Files stored locally under `{app.upload-dir}/knowledge/{kbId}/{docId}.{ext}`
- File path stored in KnowledgeDocument.filePath
- Local file is the original; extracted text is chunked and embedded

### Text Chunking

- Split by paragraph boundaries
- Target chunk size: ~500 tokens (~750 characters for Chinese text)
- Overlap: 50 tokens between consecutive chunks for context continuity

## New Dependencies

```xml
<!-- PGVector JDBC -->
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
</dependency>

<!-- Apache PDFBox -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
</dependency>

<!-- Apache POI (DOCX) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>
```

## Permission Model

Admin is determined by a `role` field added to the User entity (enum: `ROLE_USER`, `ROLE_ADMIN`). The current project only has `ROLE_USER`; a migration will add the column with a default of `ROLE_USER`.

| Action             | Owner | Admin | Other Users |
|--------------------|-------|-------|-------------|
| Create KB          | Yes   | Yes   | Yes         |
| View own KB        | Yes   | Yes   | No          |
| View public KB     | Yes   | Yes   | Yes         |
| Add documents      | Yes   | Yes   | No          |
| Delete own KB      | Yes   | Yes   | No          |
| Delete any KB      | No    | Yes   | No          |
| Delete own doc     | Yes   | Yes   | No          |
| Delete any doc     | No    | Yes   | No          |

## New Files Structure

```
src/main/java/com/gd/mealmate/
├── controller/
│   └── KnowledgeBaseController.java
├── dto/
│   ├── request/
│   │   ├── CreateKnowledgeBaseRequest.java
│   │   └── CreateDocumentRequest.java
│   └── response/
│       ├── KnowledgeBaseDto.java
│       ├── KnowledgeBaseDetailDto.java
│       └── KnowledgeDocumentDto.java
├── mapper/
│   ├── KnowledgeBaseMapper.java
│   └── KnowledgeDocumentMapper.java
├── model/
│   ├── entity/
│   │   ├── KnowledgeBase.java
│   │   ├── KnowledgeDocument.java
│   │   └── KnowledgeChunk.java
│   └── enums/
│       └── DocumentContentType.java
├── repository/
│   ├── KnowledgeBaseRepository.java
│   ├── KnowledgeDocumentRepository.java
│   └── KnowledgeChunkRepository.java
└── service/
    ├── KnowledgeBaseService.java
    ├── DocumentParseService.java
    └── EmbeddingService.java
```

## Database Setup

Enable PGVector extension in PostgreSQL:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

The `embedding` column on `knowledge_chunks` will use `vector(1024)` type. The project uses DeepSeek's API; if switching to OpenAI embeddings, use `vector(1536)`. The embedding dimension is configurable via `app.knowledge.embedding-dimension` to accommodate different models.

## Configuration

New properties in `application.yml`:

```yaml
app:
  upload:
    dir: ./uploads
  knowledge:
    chunk-size: 500
    chunk-overlap: 50
    top-k: 5
    similarity-threshold: 0.7
    embedding-dimension: 1024
```
