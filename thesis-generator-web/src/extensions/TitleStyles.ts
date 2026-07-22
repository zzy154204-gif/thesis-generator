import { Node } from '@tiptap/core'

/**
 * 图标题节点 — 渲染为 <p class="figure-title">
 * 用于在编辑器中标记 "图 1: ..." 样式的段落
 */
export const FigureTitle = Node.create({
  name: 'figureTitle',

  group: 'block',

  content: 'inline*',

  parseHTML() {
    return [{ tag: 'p.figure-title' }]
  },

  renderHTML: ({ HTMLAttributes }) => ['p', { class: 'figure-title' }, 0],

  addCommands() {
    return {
      setFigureTitle:
        () =>
        ({ commands }: { commands: any }) =>
          commands.setNode(this.name),
      toggleFigureTitle:
        () =>
        ({ commands }: { commands: any }) =>
          commands.toggleNode(this.name, 'paragraph'),
    } as any
  },
})

/**
 * 表标题节点 — 渲染为 <p class="table-title">
 * 用于在编辑器中标记 "表 1: ..." 样式的段落
 */
export const TableTitle = Node.create({
  name: 'tableTitle',

  group: 'block',

  content: 'inline*',

  parseHTML() {
    return [{ tag: 'p.table-title' }]
  },

  renderHTML: ({ HTMLAttributes }) => ['p', { class: 'table-title' }, 0],

  addCommands() {
    return {
      setTableTitle:
        () =>
        ({ commands }: { commands: any }) =>
          commands.setNode(this.name),
      toggleTableTitle:
        () =>
        ({ commands }: { commands: any }) =>
          commands.toggleNode(this.name, 'paragraph'),
    } as any
  },
})
