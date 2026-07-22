<template>
  <div class="layout">
    <!-- ===== 顶栏 ===== -->
    <header class="topbar">
      <div class="topbar-left">
        <el-icon class="menu-btn" @click="collapsed = !collapsed">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </div>
      <div class="topbar-right">
        <el-dropdown @command="onCommand">
          <span class="user-trigger">
            <el-avatar :size="28" icon="UserFilled" class="avatar" />
            <span class="username">{{ auth.user?.realName || '用户' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="body">
      <!-- ===== 侧边栏 ===== -->
      <aside class="sidebar" :class="{ collapsed }">
        <!-- Logo 区域 -->
        <div class="logo-area">
          <div class="logo-icon">📄</div>
          <span class="logo-text" v-show="!collapsed">论文生成系统</span>
        </div>
        <div class="logo-divider" v-show="!collapsed" />

        <!-- 导航菜单 -->
        <el-menu
          :default-active="activePath"
          router
          :collapse="collapsed"
          class="nav-menu"
        >
          <template v-for="item in menus" :key="item.path">
            <el-menu-item v-if="!item.children" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
              </template>
              <el-menu-item v-for="c in item.children" :key="c.path" :index="c.path">
                {{ c.label }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>

        <!-- 折叠状态下只显示缩略 Logo -->
        <div class="logo-collapsed" v-show="collapsed">
          <span class="logo-icon-small">📄</span>
        </div>
      </aside>

      <!-- ===== 内容区 ===== -->
      <main class="main">
        <div class="page">
          <slot />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Fold, Expand, Document, Files, Reading, School, ChatLineSquare, User } from '@element-plus/icons-vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

interface NavItem {
  path: string
  label: string
  icon?: any
  roles?: string[]
  children?: NavItem[]
}

const allMenus: NavItem[] = [
  { path: '/papers', label: '我的论文', icon: Document, roles: ['STUDENT'] },
  { path: '/templates', label: '模板库', icon: Files, roles: ['STUDENT'] },
  { path: '/references', label: '参考文献', icon: Reading, roles: ['STUDENT'] },
  { path: '/teacher/review', label: '待批阅', icon: ChatLineSquare, roles: ['TEACHER'] },
  { path: '/admin/colleges', label: '学院管理', icon: School, roles: ['ADMIN'] },
  { path: '/admin/templates', label: '模板管理', icon: Files, roles: ['ADMIN'] },
  { path: '/profile', label: '个人中心', icon: User },
]

const menus = computed(() =>
  allMenus.filter(m => !m.roles || m.roles.includes(auth.role || ''))
)

/** 当前激活的菜单项：优先匹配路由，若路由不在菜单中则高亮第一项 */
const activePath = computed(() => {
  const matched = menus.value.find(m => route.path.startsWith(m.path))
  return matched ? matched.path : (menus.value[0]?.path || '/papers')
})

function onCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') {
    auth.logout().then(() => router.push('/login'))
  }
}
</script>

<style scoped lang="scss">
.layout {
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

// ---- 顶栏 ----
.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--el-fill-color-blank);
  border-bottom: 1px solid var(--el-border-color-light);
  position: sticky;
  top: 0;
  z-index: 100;

  .menu-btn {
    font-size: 18px;
    cursor: pointer;
    color: var(--el-text-color-secondary);
    &:hover { color: var(--el-color-primary); }
  }

  .topbar-right { display: flex; align-items: center; }

  .user-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 10px;
    border-radius: 6px;
    &:hover { background: var(--el-fill-color); }
    .username { font-size: 14px; color: var(--el-text-color-primary); }
  }

  .avatar { background: var(--el-color-primary-light-5); }
}

.body {
  display: flex;
  min-height: calc(100vh - 56px);
}

// ---- 侧边栏 ----
.sidebar {
  width: 220px;
  background: var(--sidebar-bg, #3e2e1f);
  transition: width 0.25s ease;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.collapsed { width: 64px; }

  // ---------- Logo 区域 ----------
  .logo-area {
    height: 60px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 18px;
    flex-shrink: 0;
  }

  .logo-icon {
    font-size: 26px;
    line-height: 1;
    flex-shrink: 0;
  }

  .logo-text {
    font-size: 16px;
    font-weight: 700;
    color: var(--sidebar-text-active, #f5efe8);
    letter-spacing: 1px;
    white-space: nowrap;
  }

  .logo-divider {
    height: 1px;
    margin: 0 16px 4px;
    background: rgba(255, 255, 255, 0.10);
    flex-shrink: 0;
  }

  // ---------- 折叠时底部 Logo ----------
  .logo-collapsed {
    margin-top: auto;
    padding: 12px 0;
    text-align: center;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
  }

  .logo-icon-small {
    font-size: 24px;
    line-height: 1;
  }

  // ---------- 导航菜单 ----------
  :deep(.nav-menu) {
    border-right: none;
    background: transparent;
    width: 220px;
    flex: 1;

    .el-menu-item,
    .el-sub-menu__title {
      color: var(--sidebar-text, #c4b8a8) !important;
      height: 46px;
      line-height: 46px;
      margin: 2px 8px;
      border-radius: 6px;
      width: auto !important;
      transition: background 0.2s, color 0.2s;

      &:hover {
        background: var(--sidebar-hover, #4d3a28) !important;
        color: var(--sidebar-text-active, #f5efe8) !important;
      }

      .el-icon { color: inherit; }
    }

    .el-menu-item.is-active {
      background: var(--el-color-primary) !important;
      color: #fff !important;
      font-weight: 500;
    }

    .el-menu--inline .el-menu-item {
      background: rgba(0, 0, 0, 0.15);
      margin: 1px 8px;
      padding-left: 40px !important;
    }
  }

  &.collapsed {
    :deep(.nav-menu) { width: 64px; }

    .logo-area {
      justify-content: center;
      padding: 0;
      height: 56px;
    }

    .logo-text { display: none; }
  }
}

// ---- 内容区 ----
.main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-width: 0;
}

.page {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
