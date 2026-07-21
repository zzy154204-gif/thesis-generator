import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ===== 公开页面 =====
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { requiresAuth: false },
    },

    // ===== 学生页面 =====
    {
      path: '/papers',
      name: 'PaperList',
      component: () => import('@/views/paper/PaperList.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/papers/create',
      name: 'PaperCreate',
      component: () => import('@/views/paper/PaperCreate.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/editor/:paperId',
      name: 'PaperEditor',
      component: () => import('@/views/paper/PaperEditor.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/preview/:paperId',
      name: 'Preview',
      component: () => import('@/views/preview/PreviewPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/templates',
      name: 'StudentTemplates',
      component: () => import('@/views/student/TemplateBrowse.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/references',
      name: 'StudentReferences',
      component: () => import('@/views/student/ReferenceManage.vue'),
      meta: { requiresAuth: true },
    },

    // ===== 管理员页面 =====
    {
      path: '/admin/colleges',
      name: 'CollegeManage',
      component: () => import('@/views/admin/CollegeManage.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/admin/templates',
      name: 'TemplateList',
      component: () => import('@/views/admin/TemplateList.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/admin/templates/new',
      name: 'TemplateNew',
      component: () => import('@/views/admin/TemplateEditor.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },
    {
      path: '/admin/templates/:id',
      name: 'TemplateEdit',
      component: () => import('@/views/admin/TemplateEditor.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
    },

    // ===== 教师页面 =====
    {
      path: '/teacher/review',
      name: 'PendingList',
      component: () => import('@/views/teacher/PendingList.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },
    {
      path: '/teacher/review/:id',
      name: 'ReviewDetail',
      component: () => import('@/views/teacher/ReviewDetail.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },

    // ===== 公共页面 =====
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/user/Profile.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/export-history',
      name: 'ExportHistory',
      component: () => import('@/views/user/ExportHistory.vue'),
      meta: { requiresAuth: true },
    },

    // ===== 重定向 =====
    { path: '/', redirect: '/papers' },
    { path: '/:pathMatch(.*)*', redirect: '/papers' },
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
