import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'
import DefaultLayout from "@/layouts/DefaultLayout.vue";

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
      path: '/demo/tiptap',
      name: 'TiptapDemo',
      component: () => import('@/views/demo/TiptapDemo.vue'),
      meta: { requiresAuth: false, layout: 'default' },
    },
    {
      path: '/',
      redirect: '/papers',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/papers',
    },
    {
      path: '/admin',
      component: DefaultLayout,
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        {
          path: 'colleges',
          component: () => import('@/views/admin/CollegeManage.vue'),
          meta: { title: '学院管理' }
        },
        {
          path: 'templates',
          component: () => import('@/views/admin/TemplateList.vue'),
          meta: { title: '模板管理' }
        },
        {
          path: 'templates/new',
          component: () => import('@/views/admin/TemplateEditor.vue'),
          meta: { title: '新建模板' }
        },
        {
          path: 'templates/:id',
          component: () => import('@/views/admin/TemplateEditor.vue'),
          meta: { title: '编辑模板' }
        }

      ]
    },
    {
      path: '/teacher',
      component: DefaultLayout,
      meta: { requiresAuth: true, roles: ['TEACHER'] },
      children: [
        {
          path: 'review',
          component: () => import('@/views/teacher/PendingList.vue'),
          meta: { title: '待批阅' }
        },
        {
          path: 'review/:id',
          component: () => import('@/views/teacher/ReviewDetail.vue'),
          meta: { title: '批阅详情' }
        }
      ]
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
