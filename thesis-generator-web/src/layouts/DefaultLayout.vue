<template>
  <div class="default-layout">
    <header class="app-header">
      <div class="header-left">
        <router-link to="/papers" class="logo">📄 论文生成系统</router-link>
      </div>
      <div class="header-right">
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
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

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
.app-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

  .logo {
    font-size: 18px;
    font-weight: 600;
    color: #409EFF;
    text-decoration: none;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    .username { font-size: 14px; color: #303133; }
  }
}

.app-main {
  min-height: calc(100vh - 56px);
  background: #f5f7fa;
}
</style>
