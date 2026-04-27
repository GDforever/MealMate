import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function setupRouterGuards(router: Router) {
  router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const token = userStore.token

    if (to.meta.requiresAuth !== false && !token) {
      next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
      return
    }

    if ((to.name === 'Login' || to.name === 'Register') && token) {
      next({ name: 'Chat' })
      return
    }

    if (to.meta.title) {
      document.title = `${to.meta.title} - MealMate`
    }

    next()
  })
}
