<template>
  <div class="knowledge-detail-view">
    <div v-loading="loading" class="detail-content">
      <div class="page-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack" text>返回</el-button>
          <h2>{{ detail?.title }}</h2>
          <el-tag :type="detail?.isPublic ? 'success' : 'info'" size="small">
            {{ detail?.isPublic ? '公开' : '私有' }}
          </el-tag>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="showAddTextDialog">添加文本</el-button>
          <el-button :icon="Upload" @click="showUploadDialog">上传文件</el-button>
          <el-button type="danger" :icon="Delete" @click="handleDeleteKB">删除知识库</el-button>
        </div>
      </div>

      <p v-if="detail?.description" class="kb-description">{{ detail.description }}</p>

      <el-divider content-position="left">文档列表 ({{ detail?.documents?.length || 0 }})</el-divider>

      <el-table :data="detail?.documents || []" stripe row-key="id" @expand-change="handleExpandChange">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div v-loading="chunkLoadingMap[row.id]" class="chunk-expand">
              <template v-if="chunkMap[row.id]?.length">
                <div v-for="chunk in chunkMap[row.id]" :key="chunk.id" class="chunk-item">
                  <div class="chunk-header">
                    <el-tag size="small" type="info">分块 {{ chunk.chunkIndex + 1 }}</el-tag>
                    <span class="chunk-meta">{{ chunk.content.length }} 字符</span>
                  </div>
                  <pre class="chunk-content">{{ chunk.content }}</pre>
                </div>
              </template>
              <el-empty v-else-if="!chunkLoadingMap[row.id]" description="暂无分块数据" :image-size="40" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="contentType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="contentTypeTag(row.contentType)">
              {{ row.contentType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDeleteDoc(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && (!detail?.documents || detail.documents.length === 0)" description="暂无文档，请添加文本或上传文件" />
    </div>

    <!-- Add Text Dialog -->
    <el-dialog v-model="addTextDialogVisible" title="添加文本文档" width="600px">
      <el-form :model="addTextForm" :rules="addTextRules" ref="addTextFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="addTextForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="addTextForm.content" type="textarea" :rows="10" placeholder="请输入或粘贴文本内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTextDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAddText">添加</el-button>
      </template>
    </el-dialog>

    <!-- Upload File Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px">
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="文件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".pdf,.docx,.txt"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOCX、TXT 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus, Upload, Delete, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import type { KnowledgeBaseDetail, KnowledgeDocument, KnowledgeChunk, CreateDocumentRequest } from '@/types'
import { knowledgeApi } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const kbId = Number(route.params.id)
const loading = ref(false)
const submitting = ref(false)
const detail = ref<KnowledgeBaseDetail>()

const addTextDialogVisible = ref(false)
const addTextFormRef = ref<FormInstance>()
const addTextForm = ref<CreateDocumentRequest>({ title: '', content: '' })
const addTextRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const uploadDialogVisible = ref(false)
const uploadFormRef = ref<FormInstance>()
const uploadForm = ref({ title: '', file: null as File | null })
const uploadRef = ref()
const uploadRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

onMounted(() => { loadDetail() })

const chunkMap = ref<Record<number, KnowledgeChunk[]>>({})
const chunkLoadingMap = ref<Record<number, boolean>>({})

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await knowledgeStore.getDetail(kbId)
  } finally {
    loading.value = false
  }
}

const handleExpandChange = async (row: KnowledgeDocument, expandedRows: KnowledgeDocument[]) => {
  if (chunkMap.value[row.id]) return
  const isExpanded = expandedRows.some(r => r.id === row.id)
  if (!isExpanded) return

  chunkLoadingMap.value[row.id] = true
  try {
    chunkMap.value[row.id] = await knowledgeApi.getDocumentChunks(kbId, row.id)
  } catch {
    chunkMap.value[row.id] = []
  } finally {
    chunkLoadingMap.value[row.id] = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const contentTypeTag = (type: string) => {
  const map: Record<string, string> = { TEXT: '', PDF: 'warning', DOCX: 'success', TXT: 'info' }
  return map[type] || ''
}

const goBack = () => router.push('/knowledge')

const showAddTextDialog = () => {
  addTextForm.value = { title: '', content: '' }
  addTextDialogVisible.value = true
}

const showUploadDialog = () => {
  uploadForm.value = { title: '', file: null }
  uploadDialogVisible.value = true
}

const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    uploadForm.value.file = file.raw
    if (!uploadForm.value.title) {
      uploadForm.value.title = file.name.replace(/\.[^.]+$/, '')
    }
  }
}

const handleFileRemove = () => {
  uploadForm.value.file = null
}

const handleAddText = async () => {
  const valid = await addTextFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await knowledgeStore.addTextDocument(kbId, addTextForm.value)
    ElMessage.success('添加成功')
    addTextDialogVisible.value = false
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

const handleUpload = async () => {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }

  submitting.value = true
  try {
    await knowledgeStore.uploadFileDocument(kbId, uploadForm.value.title, uploadForm.value.file)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

const handleDeleteKB = async () => {
  try {
    await ElMessageBox.confirm('删除知识库将同时删除所有文档和向量数据，确定删除？', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await knowledgeStore.deleteKnowledgeBase(kbId)
    ElMessage.success('删除成功')
    router.push('/knowledge')
  } catch { /* cancelled */ }
}

const handleDeleteDoc = async (doc: KnowledgeDocument) => {
  try {
    await ElMessageBox.confirm(`确定删除文档「${doc.title}」？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await knowledgeStore.deleteDocument(kbId, doc.id)
    ElMessage.success('删除成功')
    await loadDetail()
  } catch { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.knowledge-detail-view {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;

  h2 {
    margin: 0 8px;
    font-size: 20px;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.kb-description {
  color: #666;
  margin: 0 0 16px 0;
  font-size: 14px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
}

.chunk-expand {
  padding: 8px 16px 16px;
}

.chunk-item {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;

  &:last-child {
    margin-bottom: 0;
  }
}

.chunk-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.chunk-meta {
  color: #909399;
  font-size: 12px;
}

.chunk-content {
  margin: 0;
  padding: 12px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  max-height: 200px;
  overflow-y: auto;
}
</style>
