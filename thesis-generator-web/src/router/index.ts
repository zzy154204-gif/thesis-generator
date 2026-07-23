import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ---- 公开 ----
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/Register.vue'),
      meta: { guest: true },
    },

    // ---- 学生 ----
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
      path: '/papers/import',
      name: 'PaperImport',
      component: () => import('@/views/paper/PaperImport.vue'),
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

    // ---- 管理员 ----
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

    // ---- 教师 ----
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
    {
      path: '/teacher/review-records',
      name: 'ReviewRecords',
      component: () => import('@/views/teacher/ReviewRecords.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
    },

    // ---- 学生 ----
    {
      path: '/submission-records',
      name: 'SubmissionRecords',
      component: () => import('@/views/student/SubmissionRecords.vue'),
      meta: { requiresAuth: true },
    },

    // ---- 公共 ----
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

    { path: '/', redirect: () => roleHome() },
    { path: '/:pathMatch(.*)*', redirect: () => roleHome() },
  ],
})

/** 根据角色返回默认首页 */
function roleHome() {
  const role = localStorage.getItem('thesis_role')
  if (role === 'TEACHER') return '/teacher/review'
  if (role === 'ADMIN') return '/admin/templates'
  return '/papers'
}

router.beforeEach((to, _from, next) => {
  const token = getToken()

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (token && to.meta.guest) {
    next(roleHome())
  } else if (to.meta.roles) {
    // 检查角色权限（从 localStorage 或解析 token）
    let role = localStorage.getItem('thesis_role')
    if (!role) {
      try {
        const token = getToken()
        if (token) {
          const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
          while (base64.length % 4) base64 += '='
          const payload = JSON.parse(decodeURIComponent(escape(atob(base64))))
          role = payload.role || ''
          if (role) localStorage.setItem('thesis_role', role)
        }
      } catch { /* fall through */ }
    }
    if (!role || !(to.meta.roles as string[]).includes(role)) {
      next(roleHome())
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
