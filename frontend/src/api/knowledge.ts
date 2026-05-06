import request from './request'
import type { KnowledgeBase, KnowledgeBaseDetail, KnowledgeDocument, KnowledgeChunk, PageResponse, CreateKnowledgeBaseRequest, CreateDocumentRequest } from '@/types'

export const knowledgeApi = {
  getList(params: { page: number; size: number; keyword?: string }) {
    return request.get<PageResponse<KnowledgeBase>>('/knowledge-bases', { params })
  },

  getDetail(id: number) {
    return request.get<KnowledgeBaseDetail>(`/knowledge-bases/${id}`)
  },

  create(data: CreateKnowledgeBaseRequest) {
    return request.post<KnowledgeBase>('/knowledge-bases', data)
  },

  delete(id: number) {
    return request.delete(`/knowledge-bases/${id}`)
  },

  addTextDocument(kbId: number, data: CreateDocumentRequest) {
    return request.post<KnowledgeDocument>(`/knowledge-bases/${kbId}/documents/text`, data)
  },

  uploadFileDocument(kbId: number, title: string, file: File) {
    const formData = new FormData()
    formData.append('title', title)
    formData.append('file', file)
    return request.post<KnowledgeDocument>(`/knowledge-bases/${kbId}/documents/file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  deleteDocument(kbId: number, docId: number) {
    return request.delete(`/knowledge-bases/${kbId}/documents/${docId}`)
  },

  getDocumentChunks(kbId: number, docId: number) {
    return request.get<KnowledgeChunk[]>(`/knowledge-bases/${kbId}/documents/${docId}/chunks`)
  }
}
