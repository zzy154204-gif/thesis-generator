<template>
  <div class="editor-sidebar left-sidebar" :style="{ width: panelWidth + 'px' }">
    <div class="sidebar-header">
      <span>章节导航</span>
      <el-button text size="small" :icon="Plus" @click="$emit('addSection')">添加</el-button>
    </div>
    <div class="section-tree">
      <el-tree
        :data="sections"
        :props="{ children: 'children', label: 'title' }"
        node-key="id"
        draggable
        highlight-current
        :allow-drop="() => true"
        @node-click="(data: ThesisSection) => $emit('sectionClick', data)"
        @node-drag-end="$emit('dragEnd')"
      >
        <template #default="{ node, data }">
          <div class="tree-node" @contextmenu.prevent="showMenu($event, data)">
            <span>{{ node.label }}</span>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="section-context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="menu-item" @click="handleAction('add', contextMenu.node!)">添加同级章节</div>
        <div class="menu-item" @click="handleAction('addChild', contextMenu.node!)">添加子章节</div>
        <div class="menu-item" @click="handleAction('rename', contextMenu.node!)">重命名</div>
        <div class="menu-item danger" @click="handleAction('delete', contextMenu.node!)">删除章节</div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { ThesisSection } from '@/types/api'

const props = defineProps<{
  sections: ThesisSection[]
  panelWidth: number
}>()

const emit = defineEmits<{
  sectionClick: [data: ThesisSection]
  addSection: [parent?: ThesisSection]
  addChildSection: [parent: ThesisSection]
  renameSection: [node: ThesisSection]
  deleteSection: [node: ThesisSection]
  dragEnd: []
}>()

const contextMenu = reactive<{ visible: boolean; x: number; y: number; node: ThesisSection | null }>({
  visible: false, x: 0, y: 0, node: null,
})

function showMenu(event: MouseEvent, data: ThesisSection) {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = data
  setTimeout(() => document.addEventListener('click', () => { contextMenu.visible = false }, { once: true }), 0)
}

function handleAction(action: string, node: ThesisSection) {
  contextMenu.visible = false
  switch (action) {
    case 'add': emit('addSection', node); break
    case 'addChild': emit('addChildSection', node); break
    case 'rename': emit('renameSection', node); break
    case 'delete': emit('deleteSection', node); break
  }
}
</script>

<style scoped lang="scss">
.editor-sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 14px;
}

.section-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-node {
  padding: 4px 0;
  font-size: 14px;
}
</style>

<style lang="scss">
.section-context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 2000;
  min-width: 160px;
  .menu-item {
    padding: 10px 16px;
    cursor: pointer;
    font-size: 13px;
    &:hover { background: #f5f7fa; }
    &.danger { color: #F56C6C; }
  }
}
</style>
