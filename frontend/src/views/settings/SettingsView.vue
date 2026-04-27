<!-- src/views/settings/SettingsView.vue -->
<template>
  <div class="settings-view">
    <h2>个人设置</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="账号设置" name="account">
        <el-card>
          <el-form :model="accountForm" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="accountForm.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="accountForm.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateAccount">更新</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 24px;">
          <template #header>
            <h3>修改密码</h3>
          </template>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="passwordForm.currentPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdatePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="口味偏好" name="preferences">
        <el-card>
          <template #header>
            <h3>选择您喜欢的口味标签</h3>
          </template>
          <el-checkbox-group v-model="selectedPreferences">
            <el-space wrap>
              <el-checkbox
                v-for="tag in availableTags"
                :key="tag"
                :label="tag"
                border
              />
            </el-space>
          </el-checkbox-group>
          <div style="margin-top: 24px;">
            <el-button type="primary" @click="handleSavePreferences">保存偏好</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="通知设置" name="notifications">
        <el-card>
          <el-form label-width="200px">
            <el-form-item label="用餐提醒">
              <el-switch v-model="notificationSettings.mealReminder" />
            </el-form-item>
            <el-form-item label="餐厅推荐">
              <el-switch v-model="notificationSettings.restaurantRecommendation" />
            </el-form-item>
            <el-form-item label="数据统计报告">
              <el-switch v-model="notificationSettings.statsReport" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveNotifications">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'

const userStore = useUserStore()
const { userInfo } = useAuth()

const activeTab = ref('account')
const passwordFormRef = ref<FormInstance>()

const accountForm = reactive({
  username: '',
  email: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const availableTags = [
  '辣', '清淡', '素食', '高蛋白', '低碳水',
  '快餐', '火锅', '烧烤', '日料', '西餐',
  '川菜', '粤菜', '湘菜', '鲁菜', '苏菜'
]

const selectedPreferences = ref<string[]>([])

const notificationSettings = reactive({
  mealReminder: true,
  restaurantRecommendation: true,
  statsReport: false
})

onMounted(() => {
  if (userInfo.value) {
    accountForm.username = userInfo.value.username
    accountForm.email = userInfo.value.email
    selectedPreferences.value = [...userInfo.value.tastePreferences]
  }
})

const handleUpdateAccount = () => {
  ElMessage.success('账号信息已更新')
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('密码修改成功')
      passwordForm.currentPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  })
}

const handleSavePreferences = async () => {
  await userStore.updatePreferences(selectedPreferences.value)
  ElMessage.success('偏好已保存')
}

const handleSaveNotifications = () => {
  ElMessage.success('通知设置已保存')
}
</script>

<style scoped lang="scss">
.settings-view {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;

  h2 {
    margin-bottom: 24px;
  }
}

:deep(.el-tabs__content) {
  padding: 24px;
}

:deep(.el-checkbox.is-bordered) {
  margin-right: 12px;
  margin-bottom: 12px;
}
</style>
