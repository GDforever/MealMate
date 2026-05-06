import { defineStore } from 'pinia'
import { ref } from 'vue'
import { knowledgeApi } from '@/api/knowledge'
import type { KnowledgeBase, KnowledgeBaseDetail, CreateKnowledgeBaseRequest, CreateDocumentRequest } from '@/types'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeBases = ref<KnowledgeBase[]>([])
  const total = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(10)
  const keyword = ref('')
  const loading = ref(false)

  const fetchList = async (page = 0) => {
    loading.value = true
    try {
      const response = await knowledgeApi.getList({
        page,
        size: pageSize.value,
        keyword: keyword.value || undefined
      })
      knowledgeBases.value = response.content
      total.value = response.totalElements
      currentPage.value = response.number
    } finally {
      loading.value = false
    }
  }

  const createKnowledgeBase = async (data: CreateKnowledgeBaseRequest) => {
    const kb = await knowledgeApi.create(data)
    knowledgeBases.value.unshift(kb)
    total.value++
    return kb
  }

  const deleteKnowledgeBase = async (id: number) => {
    await knowledgeApi.delete(id)
    knowledgeBases.value = knowledgeBases.value.filter(kb => kb.id !== id)
    total.value--
  }

  const getDetail = async (id: number): Promise<KnowledgeBaseDetail> => {
    return await knowledgeApi.getDetail(id)
  }

  const addTextDocument = async (kbId: number, data: CreateDocumentRequest) => {
    const doc = await knowledgeApi.addTextDocument(kbId, data)
    const kb = knowledgeBases.value.find(k => k.id === kbId)
    if (kb) kb.documentCount++
    return doc
  }

  const uploadFileDocument = async (kbId: number, title: string, file: File) => {
    const doc = await knowledgeApi.uploadFileDocument(kbId, title, file)
    const kb = knowledgeBases.value.find(k => k.id === kbId)
    if (kb) kb.documentCount++
    return doc
  }

  const deleteDocument = async (kbId: number, docId: number) => {
    await knowledgeApi.deleteDocument(kbId, docId)
    const kb = knowledgeBases.value.find(k => k.id === kbId)
    if (kb) kb.documentCount--
  }

  const setKeyword = (value: string) => {
    keyword.value = value
  }

  return {
    knowledgeBases,
    total,
    currentPage,
    pageSize,
    keyword,
    loading,
    fetchList,
    createKnowledgeBase,
    deleteKnowledgeBase,
    getDetail,
    addTextDocument,
    uploadFileDocument,
    deleteDocument,
    setKeyword
  }
})
