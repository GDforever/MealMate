export interface KnowledgeBase {
  id: number
  title: string
  description: string
  userId: number
  username: string
  isPublic: boolean
  documentCount: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeDocument {
  id: number
  knowledgeBaseId: number
  title: string
  contentType: 'TEXT' | 'PDF' | 'DOCX' | 'TXT'
  createdAt: string
}

export interface KnowledgeBaseDetail extends Omit<KnowledgeBase, 'documentCount'> {
  documents: KnowledgeDocument[]
}

export interface CreateKnowledgeBaseRequest {
  title: string
  description?: string
  isPublic?: boolean
}

export interface CreateDocumentRequest {
  title: string
  content: string
}

export interface KnowledgeChunk {
  id: number
  documentId: number
  chunkIndex: number
  content: string
  createdAt: string
}
