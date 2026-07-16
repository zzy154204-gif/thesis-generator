// src/config/menu.ts
import type { Component } from 'vue'
import {
  School, Document, Files, User, Setting,
  ChatLineSquare, Tickets, EditPen
} from '@element-plus/icons-vue'

export interface MenuItem {
  path: string
  title: string
  icon: Component
  roles?: string[]  // 不填表示所有角色可见
  children?: MenuItem[]
}

export const menuConfig: MenuItem[] = [
  // ===== 公共菜单 =====
  { path: '/profile', title: '个人中心', icon: User },

  // ===== 管理员菜单 =====
  { path: '/admin/colleges', title: '学院管理', icon: School, roles: ['ADMIN'] },
  { path: '/admin/templates', title: '模板管理', icon: Files, roles: ['ADMIN'] },
  { path: '/admin/settings', title: '系统设置', icon: Setting, roles: ['ADMIN'] },

  // ===== 学生菜单 =====
  { path: '/papers', title: '我的论文', icon: Document, roles: ['STUDENT'] },
  { path: '/templates', title: '模板库', icon: Files, roles: ['STUDENT'] },
  { path: '/references', title: '参考文献', icon: Tickets, roles: ['STUDENT'] },

  // ===== 教师菜单 =====
  { path: '/teacher/review', title: '待批阅', icon: ChatLineSquare, roles: ['TEACHER'] },
  { path: '/teacher/history', title: '批阅历史', icon: Tickets, roles: ['TEACHER'] },
]
