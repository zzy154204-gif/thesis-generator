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
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/papers/create',
      name: 'PaperCreate',
      component: () => import('@/views/paper/PaperCreate.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/papers/import',
      name: 'PaperImport',
      component: () => import('@/views/paper/PaperImport.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/editor/:paperId',
      name: 'PaperEditor',
      component: () => import('@/views/paper/PaperEditor.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/preview/:paperId',
      name: 'Preview',
      component: () => import('@/views/preview/PreviewPage.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/templates',
      name: 'StudentTemplates',
      component: () => import('@/views/student/TemplateBrowse.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/references',
      name: 'StudentReferences',
      component: () => import('@/views/student/ReferenceManage.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
    },
    {
      path: '/submission-records',
      name: 'SubmissionRecords',
      component: () => import('@/views/student/SubmissionRecords.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT'] },
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

    // ---- 公共（所有角色均可访问）----
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/user/Profile.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
    },
    {
      path: '/export-history',
      name: 'ExportHistory',
      component: () => import('@/views/user/ExportHistory.vue'),
      meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
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

  // 从 localStorage 或 JWT 中提取角色
  let role = localStorage.getItem('thesis_role')
  if (!role && token) {
    try {
      const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
      const padded = b64 + '==='.slice(0, (4 - b64.length % 4) % 4)
      const payload = JSON.parse(decodeURIComponent(escape(atob(padded))))
      role = payload.role || ''
      if (role) localStorage.setItem('thesis_role', role)
    } catch { /* fall through */ }
  }

  // 未登录 → 跳转登录
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录 → 不允许访问登录/注册页
  if (token && to.meta.guest) {
    next(roleHome())
    return
  }

  // 角色权限校验：所有需鉴权的路由都必须声明 roles
  if (to.meta.requiresAuth && to.meta.roles) {
    if (!role || !(to.meta.roles as string[]).includes(role)) {
      next(roleHome())
      return
    }
  }

  // 兜底：要求鉴权但未声明 roles 的路由，拒绝访问
  if (to.meta.requiresAuth && !to.meta.roles) {
    console.warn(`路由 ${to.path} 未声明 roles 权限，已拦截`)
    next(roleHome())
    return
  }

  next()
})

export default router
