# MealMate Phase 3 — Frontend Design

**Date:** 2026-04-24
**Phase:** 3 of 3 (Frontend)
**Status:** Approved

## Overview

为 MealMate 构建完整的前端应用，采用 Vue 3 + Element Plus + TypeScript 技术栈。前端与已完成的 Phase 1（后端核心）和 Phase 2（Spring AI 集成）对接，实现 AI 对话、用餐记录管理、餐厅搜索、数据统计等完整功能。采用同仓库 monorepo 结构，前后端代码集中管理。

## Tech Stack

| Component | Choice |
|-----------|--------|
| Runtime | Vue 3.4+ |
| Language | TypeScript 5.3+ |
| Build Tool | Vite 5.0+ |
| Router | Vue Router 4.2+ |
| State Management | Pinia 2.1+ |
| UI Framework | Element Plus 2.5+ |
| HTTP Client | Axios 1.6+ |
| Date Library | Day.js 1.11+ |
| Charts | ECharts 5.5+ + Vue-ECharts 6.6+ |
| Markdown | markdown-it 14.0+ |

## Project Structure

```
MealMate/
├── backend/                          # 后端代码（现有）
├── frontend/                         # 前端代码（新增）
│   ├── public/
│   │   ├── favicon.ico
│   │   └── logo.png
│   ├── src/
│   │   ├── assets/                   # 静态资源
│   │   │   ├── images/
│   │   │   └── styles/
│   │   │       ├── index.scss        # 全局样式
│   │   │       ├── variables.scss    # SCSS 变量
│   │   │       └── element.scss      # Element Plus 覆盖样式
│   │   ├── components/               # 通用组件
│   │   │   ├── common/
│   │   │   │   ├── AppHeader.vue
│   │   │   │   ├── AppFooter.vue
│   │   │   │   └── LoadingSpinner.vue
│   │   │   ├── chat/
│   │   │   │   ├── ChatMessage.vue
│   │   │   │   ├── ChatInput.vue
│   │   │   │   ├── SessionList.vue
│   │   │   │   └── SSEHandler.ts
│   │   │   ├── meal/
│   │   │   │   ├── MealCard.vue
│   │   │   │   ├── MealCalendar.vue
│   │   │   │   └── MealForm.vue
│   │   │   └── restaurant/
│   │   │       ├── RestaurantCard.vue
│   │   │       ├── RestaurantList.vue
│   │   │       └── MapView.vue
│   │   ├── composables/              # Composables
│   │   │   ├── useAuth.ts
│   │   │   ├── useChat.ts
│   │   │   ├── useSSE.ts
│   │   │   └── useDebounce.ts
│   │   ├── layouts/                  # 布局组件
│   │   │   ├── DefaultLayout.vue
│   │   │   └── EmptyLayout.vue
│   │   ├── router/                   # 路由配置
│   │   │   ├── index.ts
│   │   │   └── guards.ts
│   │   ├── stores/                   # Pinia Stores
│   │   │   ├── user.ts
│   │   │   ├── chat.ts
│   │   │   ├── meal.ts
│   │   │   └── restaurant.ts
│   │   ├── api/                      # API 请求
│   │   │   ├── request.ts            # Axios 实例配置
│   │   │   ├── auth.ts
│   │   │   ├── chat.ts
│   │   │   ├── meal.ts
│   │   │   ├── restaurant.ts
│   │   │   └── types.ts              # API 类型定义
│   │   ├── views/                    # 页面组件
│   │   │   ├── auth/
│   │   │   │   ├── LoginView.vue
│   │   │   │   └── RegisterView.vue
│   │   │   ├── chat/
│   │   │   │   └── ChatView.vue
│   │   │   ├── meal/
│   │   │   │   ├── MealListView.vue
│   │   │   │   ├── MealCalendarView.vue
│   │   │   │   └── MealStatsView.vue
│   │   │   ├── restaurant/
│   │   │   │   ├── RestaurantListView.vue
│   │   │   │   └── RestaurantDetailView.vue
│   │   │   └── settings/
│   │   │       └── SettingsView.vue
│   │   ├── utils/                    # 工具函数
│   │   │   ├── date.ts
│   │   │   ├── storage.ts
│   │   │   └── validate.ts
│   │   ├── types/                    # TypeScript 类型
│   │   │   ├── user.ts
│   │   │   ├── chat.ts
│   │   │   ├── meal.ts
│   │   │   └── restaurant.ts
│   │   ├── App.vue
│   │   └── main.ts
│   ├── .env.development
│   ├── .env.production
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── README.md
├── pom.xml
└── README.md
```

## Routes

```typescript
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { layout: 'empty', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { layout: 'empty', requiresAuth: false }
  },
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/chat' },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: 'AI 对话' }
      },
      {
        path: 'meals',
        name: 'MealList',
        component: () => import('@/views/meal/MealListView.vue'),
        meta: { title: '用餐记录' }
      },
      {
        path: 'meals/calendar',
        name: 'MealCalendar',
        component: () => import('@/views/meal/MealCalendarView.vue'),
        meta: { title: '用餐日历' }
      },
      {
        path: 'meals/stats',
        name: 'MealStats',
        component: () => import('@/views/meal/MealStatsView.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'restaurants',
        name: 'RestaurantList',
        component: () => import('@/views/restaurant/RestaurantListView.vue'),
        meta: { title: '餐厅搜索' }
      },
      {
        path: 'restaurants/:id',
        name: 'RestaurantDetail',
        component: () => import('@/views/restaurant/RestaurantDetailView.vue'),
        meta: { title: '餐厅详情' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '个人设置' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/chat'
  }
]
```

## Pinia Stores

### UserStore

```typescript
interface UserState {
  token: string | null
  userInfo: User | null
  tastePreferences: string[]
}

// Actions:
// - login(credentials: LoginRequest)
// - register(data: RegisterRequest)
// - fetchUserInfo()
// - updatePreferences(preferences: string[])
// - logout()

// Persisted: token, tastePreferences
```

### ChatStore

```typescript
interface ChatState {
  sessions: ChatSession[]
  currentSessionId: number | null
  messages: Map<number, ChatMessage[]>
  isLoading: boolean
  streamingContent: string
}

// Actions:
// - fetchSessions()
// - createSession()
// - deleteSession(id: number)
// - sendMessage(message: string, sessionId?: number)
// - appendMessage(sessionId: number, role: string, content: string)
// - setStreamingContent(delta: string)
// - clearStreamingContent()
```

### MealStore

```typescript
interface MealState {
  records: MealRecord[]
  total: number
  currentPage: number
  filters: MealFilters
  selectedDate: string | null
}

// Actions:
// - fetchRecords(page: number)
// - createRecord(data: MealRecordRequest)
// - updateRecord(id: number, data: MealRecordRequest)
// - deleteRecord(id: number)
// - setFilters(filters: Partial<MealFilters>)
// - clearFilters()
```

### RestaurantStore

```typescript
interface RestaurantState {
  list: Restaurant[]
  searchParams: RestaurantSearchParams
  selectedRestaurant: Restaurant | null
  userLocation: Location | null
}

// Actions:
// - searchNearby(params: RestaurantSearchParams)
// - fetchDetail(id: number)
// - getCurrentLocation()
// - setUserLocation(location: Location)
```

## API Integration

### Axios Configuration

- Base URL: `/api` (开发环境代理到 `http://localhost:8080/api`)
- Timeout: 30 seconds
- Request interceptor: 自动添加 JWT token
- Response interceptor: 统一错误处理

### Error Handling

| HTTP Status | Handling |
|-------------|----------|
| 401 | 清除 token，跳转登录页 |
| 403 | 提示无权限 |
| 404 | 提示资源不存在 |
| 500 | 提示服务器错误 |
| Network Error | 提示网络连接失败 |

### SSE Handling

使用 Fetch API 处理 SSE 流：

```typescript
// composables/useSSE.ts
export function useSSE() {
  const connect = (url: string, handlers: SSEHandlers) => {
    // EventSource connection setup
    // Handlers: onOpen, onMessage, onDone, onError
  }
}
```

## Page Components

### 1. ChatView（AI 对话页）

- 左侧会话列表（可收起）
- 消息流式打字效果
- Markdown 渲染
- 创建/切换/删除会话

### 2. MealListView（用餐记录列表）

- 筛选栏（日期范围、用餐类型、搜索）
- 卡片式记录展示
- 分页

### 3. MealCalendarView（用餐日历）

- 月历视图
- 日期标记显示当日用餐
- 点击日期查看详情

### 4. MealStatsView（数据统计）

- 用餐趋势折线图（30天）
- 用餐类型分布饼图
- 口味偏好词云
- 热门餐厅 TOP10

### 5. RestaurantListView（餐厅搜索）

- 位置自动获取
- 距离、菜系、价格筛选
- 列表+地图分屏布局

### 6. RestaurantDetailView（餐厅详情）

- 餐厅基本信息
- 用户评价（如有）
- 地图定位
- 添加到用餐记录快捷操作

### 7. SettingsView（个人设置）

- Tab 切换：账号设置、口味偏好、通知设置
- 用户名、邮箱修改
- 密码修改
- 偏好标签多选

## Configuration Files

### vite.config.ts

```typescript
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          'echarts': ['echarts']
        }
      }
    }
  }
})
```

### Environment Variables

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api

# .env.production
VITE_API_BASE_URL=/api
```

## Package.json

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "pinia-plugin-persistedstate": "^3.2.0",
    "element-plus": "^2.5.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.6.0",
    "dayjs": "^1.11.0",
    "echarts": "^5.5.0",
    "vue-echarts": "^6.6.0",
    "markdown-it": "^14.0.0"
  }
}
```

## Development Commands

```bash
# 后端启动
cd backend
../mvnw spring-boot:run

# 前端启动（新终端）
cd frontend
npm install
npm run dev

# 生产构建
npm run build
```

## Phase 3 Deliverables

1. 完整的 Vue 3 + TypeScript + Element Plus 前端应用
2. 8 个核心页面（登录、注册、聊天、用餐记录、日历、统计、餐厅、设置）
3. SSE 流式聊天功能
4. Pinia 状态管理（4 个 Store）
5. Axios API 封装 + 统一错误处理
6. 路由权限守卫
7. 数据统计图表（ECharts）
8. 完整的类型定义
9. 响应式布局
10. 开发/生产环境配置
