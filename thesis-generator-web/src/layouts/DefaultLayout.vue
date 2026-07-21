<template>
  <div class="default-layout">
    <!-- ===== 顶栏 ===== -->
    <header class="app-header">
      <div class="header-left">
        <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <router-link to="/papers" class="logo">📄 论文生成系统</router-link>
      </div>

      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="30" icon="UserFilled" class="user-avatar" />
            <span class="username">{{ authStore.user?.realName || '用户' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="exportHistory">导出历史</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- ===== 主体 ===== -->
    <div class="body-wrap">
      <!-- 侧边栏 -->
      <aside class="app-sidebar" :class="{ collapsed: isCollapsed }">
        <el-menu
          :default-active="route.path"
          router
          :collapse="isCollapsed"
          class="sidebar-menu"
        >
          <template v-for="item in filteredMenus" :key="item.path">
            <el-sub-menu v-if="item.children?.length" :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children!" :key="child.path" :index="child.path">
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </aside>

      <!-- 内容区 -->
      <main class="app-main">
        <div class="page-container">
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
import { menuConfig } from '@/config/menu'
import { Fold, Expand } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)

const filteredMenus = computed(() => {
  const role = authStore.user?.role
  return menuConfig.filter((item) => {
    if (!item.roles) return true
    return item.roles.includes(role || '')
  })
})

function handleCommand(command: string) {
  if (command === 'profile') router.push('/profile')
  else if (command === 'exportHistory') router.push('/export-history')
  else if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.default-layout {
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

// ---- 顶栏 ----
.app-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fdfbf8;
  border-bottom: 1px solid var(--el-border-color-light);
  position: sticky;
  top: 0;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .collapse-btn {
    font-size: 18px;
    cursor: pointer;
    color: var(--el-text-color-secondary);
    transition: color 0.2s;
    &:hover { color: var(--el-color-primary); }
  }

  .logo {
    font-size: 17px;
    font-weight: 600;
    color: var(--el-color-primary);
    text-decoration: none;
    letter-spacing: 0.5px;
  }

  .header-right {
    display: flex;
    align-items: center;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 10px;
    border-radius: 6px;
    transition: background 0.2s;
    &:hover { background: var(--el-fill-color); }
    .username { font-size: 14px; color: var(--el-text-color-primary); }
  }

  .user-avatar {
    background: var(--el-color-primary-light-5);
  }
}

// ---- 主体：侧边栏 + 内容 ----
.body-wrap {
  display: flex;
  min-height: calc(100vh - 56px);
}

// ---- 侧边栏 ----
.app-sidebar {
  width: 200px;
  background: var(--sidebar-bg);
  transition: width 0.25s ease;
  flex-shrink: 0;
  overflow: hidden;

  &.collapsed { width: 64px; }

  :deep(.sidebar-menu) {
    border-right: none;
    background: transparent;
    height: 100%;

    &:not(.el-menu--collapse) { width: 200px; }

    .el-menu-item,
    .el-sub-menu__title {
      color: var(--sidebar-text) !important;
      height: 48px;
      line-height: 48px;
      transition: background 0.2s, color 0.2s;

      &:hover {
        background: var(--sidebar-hover) !important;
        color: var(--sidebar-text-active) !important;
      }

      .el-icon { color: inherit; }
    }

    .el-menu-item.is-active {
      background: var(--el-color-primary) !important;
      color: #fff !important;
      font-weight: 500;
    }

    .el-sub-menu__title:hover {
      background: var(--sidebar-hover) !important;
    }

    .el-menu--inline .el-menu-item {
      background: rgba(0, 0, 0, 0.12);
      padding-left: 56px !important;
      height: 42px;
      line-height: 42px;
    }
  }
}

// ---- 内容区 ----
.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-width: 0;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
