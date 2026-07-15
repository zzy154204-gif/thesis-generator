import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { requiresAuth: false, layout: 'auth' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { requiresAuth: false, layout: 'auth' },
    },
    {
      path: '/papers',
      name: 'PaperList',
      component: () => import('@/views/paper/PaperList.vue'),
      meta: { requiresAuth: true, layout: 'default' },
    },
    {
      path: '/papers/create',
      name: 'PaperCreate',
      component: () => import('@/views/paper/PaperCreate.vue'),
      meta: { requiresAuth: true, layout: 'default' },
    },
    {
      path: '/editor/:paperId',
      name: 'PaperEditor',
      component: () => import('@/views/paper/PaperEditor.vue'),
      meta: { requiresAuth: true, layout: 'editor' },
    },
    {
      path: '/preview/:paperId',
      name: 'Preview',
      component: () => import('@/views/preview/PreviewPage.vue'),
      meta: { requiresAuth: true, layout: 'default' },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/user/Profile.vue'),
      meta: { requiresAuth: true, layout: 'default' },
    },
    {
      path: '/export-history',
      name: 'ExportHistory',
      component: () => import('@/views/user/ExportHistory.vue'),
      meta: { requiresAuth: true, layout: 'default' },
    },
    {
      path: '/',
      redirect: '/papers',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/papers',
    },
  ],
})

// 路由鉴权守卫
router.beforeEach((to, from, next) => {
  const token = getToken()

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (token && (to.path === '/login' || to.path === '/register')) {
    next({ path: '/papers' })
  } else {
    next()
  }
})

export default router
