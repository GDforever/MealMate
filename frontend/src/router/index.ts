import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
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
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/chat'
      },
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
        path: 'meals/photo',
        name: 'FoodCamera',
        component: () => import('@/views/meal/FoodCameraView.vue'),
        meta: { title: '拍照记录' }
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
        path: 'knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/KnowledgeListView.vue'),
        meta: { title: '知识库' }
      },
      {
        path: 'knowledge/:id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/knowledge/KnowledgeDetailView.vue'),
        meta: { title: '知识库详情' }
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

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
