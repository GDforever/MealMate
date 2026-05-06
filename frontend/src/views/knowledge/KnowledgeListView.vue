<template>
  <div class="knowledge-list-view">
    <div class="page-header">
      <h2>知识库</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog">创建知识库</el-button>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索知识库"
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
        style="width: 300px"
      />
      <el-button :icon="Search" @click="handleSearch">搜索</el-button>
    </div>

    <div v-loading="loading" class="kb-list">
      <el-row :gutter="16">
        <el-col v-for="kb in knowledgeBases" :key="kb.id" :xs="24" :sm="12" :md="8">
          <el-card class="kb-card" shadow="hover" @click="goToDetail(kb.id)">
            <template #header>
              <div class="card-header">
                <span class="kb-title">{{ kb.title }}</span>
                <el-tag :type="kb.isPublic ? 'success' : 'info'" size="small">
                  {{ kb.isPublic ? '公开' : '私有' }}
                </el-tag>
              </div>
            </template>
            <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
            <div class="card-footer">
              <span class="doc-count">
                <el-icon><Document /></el-icon>
                {{ kb.documentCount }} 篇文档
              </span>
              <span class="kb-time">{{ formatDate(kb.createdAt) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库" />
    </div>

    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="createDialogVisible" title="创建知识库" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入知识库标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入描述（可选）" />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch v-model="createForm.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import type { CreateKnowledgeBaseRequest } from '@/types'

const router = useRouter()
const knowledgeStore = useKnowledgeStore()

const loading = computed(() => knowledgeStore.loading)
const knowledgeBases = computed(() => knowledgeStore.knowledgeBases)
const total = computed(() => knowledgeStore.total)
const pageSize = computed(() => knowledgeStore.pageSize)
const currentPageNum = computed({
  get: () => knowledgeStore.currentPage + 1,
  set: (v) => { knowledgeStore.currentPage = v - 1 }
})

const searchKeyword = ref('')
const createDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = ref<CreateKnowledgeBaseRequest>({
  title: '',
  description: '',
  isPublic: false
})

const createRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

onMounted(() => {
  knowledgeStore.fetchList(0)
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const handleSearch = () => {
  knowledgeStore.setKeyword(searchKeyword.value)
  knowledgeStore.fetchList(0)
}

const handlePageChange = () => {
  knowledgeStore.fetchList(knowledgeStore.currentPage)
}

const goToDetail = (id: number) => {
  router.push(`/knowledge/${id}`)
}

const showCreateDialog = () => {
  createForm.value = { title: '', description: '', isPublic: false }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await knowledgeStore.createKnowledgeBase(createForm.value)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.knowledge-list-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h2 { margin: 0; }
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.kb-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-title {
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.kb-desc {
  color: #666;
  font-size: 14px;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #999;
  font-size: 13px;

  .doc-count {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
