<!-- src/layouts/DefaultLayout.vue -->
<template>
  <div class="default-layout">
    <!-- ===== 顶栏 ===== -->
    <header class="app-header">
      <div class="header-left">
        <!-- 折叠按钮 -->
        <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <!-- Logo / 项目名称 -->
        <router-link to="/papers" class="logo">📄 论文生成系统</router-link>
      </div>

      <div class="header-right">
        <!-- 用户信息 + 下拉菜单 -->
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" icon="UserFilled" />
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
          :default-active="$route.path"
          router
          :collapse="isCollapsed"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <template v-for="item in filteredMenus" :key="item.path">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.path"
                :index="child.path"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>

            <!-- 无子菜单 -->
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </aside>

      <!-- 内容区 -->
      <main class="app-main">
        <slot />
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

// ===== 状态 =====
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)

// ===== 计算属性 =====
// 根据用户角色过滤菜单
const filteredMenus = computed(() => {
  const role = authStore.user?.role
  return menuConfig.filter((item) => {
    // 如果菜单项没有 roles 字段，说明是公共菜单，所有人可见
    if (!item.roles) return true
    // 否则检查当前角色是否在允许列表中
    return item.roles.includes(role || '')
  })
})

// ===== 方法 =====
// 下拉菜单命令处理
function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'exportHistory') {
    router.push('/export-history')
  } else if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.default-layout {
  min-height: 100vh;
  background: #f0f2f5;
}

// ---- 顶栏 ----
.app-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: #606266;
    transition: color 0.2s;

    &:hover {
      color: #409eff;
    }
  }

  .logo {
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
    text-decoration: none;
    white-space: nowrap;
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
    padding: 4px 8px;
    border-radius: 4px;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    .username {
      font-size: 14px;
      color: #303133;
    }
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
  background: #304156;
  transition: width 0.3s ease;
  flex-shrink: 0;
  overflow: hidden; // 防止折叠时内容溢出

  &.collapsed {
    width: 64px;
  }

  :deep(.el-menu) {
    border-right: none;
    height: 100%;
    width: 100%;

    .el-menu-item,
    .el-sub-menu__title {
      // 确保文字不换行
      white-space: nowrap;
    }

    // 折叠时隐藏菜单项文字
    &.el-menu--collapse {
      .el-menu-item span,
      .el-sub-menu__title span {
        display: none;
      }
    }
  }
}

// ---- 内容区 ----
.app-main {
  flex: 1;
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
  min-width: 0; // 防止 flex 溢出
}
</style>
