import Image from '@tiptap/extension-image'
import type { NodeView as NodeViewType, ViewMutationRecord } from '@tiptap/pm/view'
import type { Editor } from '@tiptap/core'
import type { Node as ProseMirrorNode } from '@tiptap/pm/model'

/**
 * 可拖拽缩放的图片 NodeView（原生 ProseMirror 实现，无 Vue 依赖）
 *
 * 直接控制 DOM 元素，在拖拽期间绕过框架响应式系统，
 * 确保 mousedown/mousemove/mouseup 事件链路完整可靠。
 */
class ResizableImageNodeView implements NodeViewType {
  dom: HTMLElement
  private img: HTMLImageElement
  private handles: HTMLElement[] = []

  private editor: Editor
  private node: ProseMirrorNode
  private getPos: () => number
  private view: any

  // 拖拽状态
  private dragging = false
  private startX = 0
  private startW = 0
  private currentW = 0

  // 绑定后的函数引用，用于 removeEventListener
  private boundOnDrag: (e: MouseEvent) => void
  private boundStopDrag: (e: MouseEvent) => void
  private boundOnTouchDrag: (e: TouchEvent) => void
  private boundStopTouchDrag: (e: TouchEvent) => void

  constructor(node: ProseMirrorNode, editor: Editor, getPos: () => number, view: any) {
    this.editor = editor
    this.node = node
    this.getPos = getPos
    this.view = view

    // 绑定事件函数（保持引用一致以便解绑）
    this.boundOnDrag = this.onDrag.bind(this)
    this.boundStopDrag = this.stopDrag.bind(this)
    this.boundOnTouchDrag = this.onTouchDrag.bind(this)
    this.boundStopTouchDrag = this.stopTouchDrag.bind(this)

    // ---- 构建 DOM ----
    const wrapper = document.createElement('span')
    wrapper.className = 'resizable-image-wrapper'
    wrapper.style.cssText = 'display:inline-block;line-height:0;position:relative;'

    const container = document.createElement('span')
    container.className = 'image-container'
    container.style.cssText = 'display:inline-block;position:relative;line-height:0;'

    this.img = document.createElement('img')
    this.img.className = 'resizable-image'
    this.img.draggable = false
    this.img.style.cssText = 'display:block;max-width:100%;height:auto;user-select:none;-webkit-user-drag:none;border-radius:2px;'

    // 设置图片属性
    const src = node.attrs.src
    if (src) this.img.src = src
    if (node.attrs.alt) this.img.alt = node.attrs.alt
    if (node.attrs.title) this.img.title = node.attrs.title

    // 初始宽度
    const initW = node.attrs.width ? Number(node.attrs.width) : 0
    this.currentW = initW
    if (initW > 0) {
      this.img.style.width = initW + 'px'
      this.img.setAttribute('width', String(initW))
    }

    // 加载后同步自然宽
    this.img.addEventListener('load', () => {
      if (!this.currentW && this.img.naturalWidth) {
        this.currentW = this.img.naturalWidth
        this.img.style.width = this.currentW + 'px'
      }
    })

    container.appendChild(this.img)
    wrapper.appendChild(container)

    // ---- 四个缩放手柄 ----
    const positions = ['nw', 'ne', 'sw', 'se'] as const
    for (const pos of positions) {
      const handle = document.createElement('span')
      handle.className = `resize-handle ${pos}`
      handle.draggable = false
      handle.style.cssText = this.getHandleStyles(pos)
      // 使用 capture: true 确保在 ProseMirror 之前拦截事件
      handle.addEventListener('mousedown', (e: MouseEvent) => this.startResize(e), { capture: true })
      handle.addEventListener('touchstart', (e: TouchEvent) => this.startTouchResize(e), { passive: false, capture: true })
      container.appendChild(handle)
      this.handles.push(handle)
    }

    this.dom = wrapper

    // 默认隐藏手柄，直到选中
    this.setHandlesVisible(false)
  }

  private getHandleStyles(pos: string): string {
    const base = 'position:absolute;width:14px;height:14px;background:#fff;border:2px solid #409eff;border-radius:2px;z-index:10;box-sizing:border-box;-webkit-user-drag:none;user-select:none;touch-action:none;'
    switch (pos) {
      case 'nw': return base + 'top:-7px;left:-7px;cursor:nw-resize;'
      case 'ne': return base + 'top:-7px;right:-7px;cursor:ne-resize;'
      case 'sw': return base + 'bottom:-7px;left:-7px;cursor:sw-resize;'
      case 'se': return base + 'bottom:-7px;right:-7px;cursor:se-resize;'
      default: return base
    }
  }

  private setHandlesVisible(visible: boolean) {
    for (const h of this.handles) {
      h.style.display = visible ? '' : 'none'
    }
  }

  // ==================== 拖拽缩放（鼠标） ====================

  private startResize(e: MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    e.stopImmediatePropagation()

    const w = this.currentW || this.img.naturalWidth || 300
    this.startX = e.clientX
    this.startW = w
    this.dragging = true

    // 设置全局 CSS 防止拖拽过程中出现选中/拖拽光标
    document.body.style.userSelect = 'none'
    document.body.style.webkitUserSelect = 'none'
    document.body.style.cursor = this.getResizeCursor(e.target as HTMLElement)

    // 使用 window 级别监听，确保鼠标移出编辑器区域依然能响应
    window.addEventListener('mousemove', this.boundOnDrag)
    window.addEventListener('mouseup', this.boundStopDrag)
  }

  private getResizeCursor(target: HTMLElement | null): string {
    if (!target) return 'se-resize'
    for (const cls of target.classList) {
      if (cls === 'nw' || cls === 'se') return 'nwse-resize'
      if (cls === 'ne' || cls === 'sw') return 'nesw-resize'
    }
    return 'se-resize'
  }

  private onDrag(e: MouseEvent) {
    e.preventDefault()
    if (!this.dragging) return
    const dx = e.clientX - this.startX
    this.applyWidth(this.startW + dx)
  }

  private stopDrag(_e: MouseEvent) {
    this.dragging = false

    window.removeEventListener('mousemove', this.boundOnDrag)
    window.removeEventListener('mouseup', this.boundStopDrag)

    // 恢复全局 CSS
    document.body.style.userSelect = ''
    document.body.style.webkitUserSelect = ''
    document.body.style.cursor = ''

    this.saveWidth()
  }

  // ==================== 拖拽缩放（触屏） ====================

  private startTouchResize(e: TouchEvent) {
    e.preventDefault()
    e.stopPropagation()
    e.stopImmediatePropagation()

    const touch = e.touches[0]
    if (!touch) return

    const w = this.currentW || this.img.naturalWidth || 300
    this.startX = touch.clientX
    this.startW = w
    this.dragging = true

    // 触屏也锁住 body 滚动/缩放
    document.body.style.overflow = 'hidden'
    document.body.style.touchAction = 'none'

    window.addEventListener('touchmove', this.boundOnTouchDrag, { passive: false })
    window.addEventListener('touchend', this.boundStopTouchDrag)
    window.addEventListener('touchcancel', this.boundStopTouchDrag)
  }

  private onTouchDrag(e: TouchEvent) {
    e.preventDefault()
    if (!this.dragging) return
    const touch = e.touches[0]
    if (!touch) return
    const dx = touch.clientX - this.startX
    this.applyWidth(this.startW + dx)
  }

  private stopTouchDrag(_e: TouchEvent) {
    this.dragging = false

    window.removeEventListener('touchmove', this.boundOnTouchDrag)
    window.removeEventListener('touchend', this.boundStopTouchDrag)
    window.removeEventListener('touchcancel', this.boundStopTouchDrag)

    document.body.style.overflow = ''
    document.body.style.touchAction = ''

    this.saveWidth()
  }

  // ==================== 宽高计算 ====================

  private applyWidth(raw: number) {
    let newW = Math.round(raw)
    if (newW < 50) newW = 50
    if (newW > 1200) newW = 1200
    this.currentW = newW

    // 直接操作 DOM，确保即时生效
    this.img.style.width = newW + 'px'
    this.img.setAttribute('width', String(newW))
  }

  private saveWidth() {
    if (this.currentW <= 0) return
    const rounded = Math.round(this.currentW)

    // 通过 ProseMirror Transaction 更新节点属性
    const pos = this.getPos()
    if (typeof pos !== 'number') return

    this.view.dispatch(
      this.view.state.tr.setNodeMarkup(pos, undefined, {
        ...this.node.attrs,
        width: rounded,
      })
    )
  }

  // ==================== NodeView 生命周期 ====================

  update(newNode: ProseMirrorNode): boolean {
    if (newNode.type !== this.node.type) return false

    this.node = newNode

    // 更新 src（处理 undo/redo 场景）
    if (newNode.attrs.src !== this.img.src) {
      this.img.src = newNode.attrs.src
    }

    // 更新宽度（处理外部修改或历史回退）
    const newWidth = newNode.attrs.width ? Number(newNode.attrs.width) : 0
    if (newWidth > 0 && newWidth !== this.currentW) {
      this.currentW = newWidth
      this.img.style.width = newWidth + 'px'
      this.img.setAttribute('width', String(newWidth))
    } else if (!newWidth && this.img.naturalWidth) {
      // width 被清空时恢复自然宽
      this.currentW = this.img.naturalWidth
      this.img.style.width = this.img.naturalWidth + 'px'
    }

    return true
  }

  selectNode() {
    this.dom.classList.add('ProseMirror-selectednode')
    this.setHandlesVisible(true)
  }

  deselectNode() {
    this.dom.classList.remove('ProseMirror-selectednode')
    this.setHandlesVisible(false)
  }

  stopEvent(e: Event): boolean {
    // 阻止所有缩放手柄上的事件被 ProseMirror 消费
    // 阻止图片拖拽相关事件（mousedown/mousemove/click）防止 ProseMirror 干涉
    const target = e.target as HTMLElement
    if (target.closest('.resize-handle')) {
      return true
    }
    // 拖拽过程中所有事件都拦截，防止 ProseMirror 中途介入
    if (this.dragging) {
      return true
    }
    return false
  }

  ignoreMutation(mutation: ViewMutationRecord | { type: 'selection'; target: Node }): boolean {
    // 允许 img 属性的 DOM 变更（宽度等）
    if (mutation.type === 'attributes' && (mutation.target as HTMLElement).tagName === 'IMG') {
      return true
    }
    // selection 变更不拦截
    if (mutation.type === 'selection') return true
    return false
  }

  destroy() {
    // 清理全局 window 级别的事件监听
    window.removeEventListener('mousemove', this.boundOnDrag)
    window.removeEventListener('mouseup', this.boundStopDrag)
    window.removeEventListener('touchmove', this.boundOnTouchDrag)
    window.removeEventListener('touchend', this.boundStopTouchDrag)
    window.removeEventListener('touchcancel', this.boundStopTouchDrag)

    // 恢复 body 样式
    document.body.style.userSelect = ''
    document.body.style.webkitUserSelect = ''
    document.body.style.cursor = ''
    document.body.style.overflow = ''
    document.body.style.touchAction = ''

    this.dragging = false
    this.setHandlesVisible(false)
  }
}

// ==================== Tiptap Extension ====================

export const ResizableImage = Image.extend({
  addAttributes() {
    return {
      src: { default: null },
      alt: { default: null },
      title: { default: null },
      width: {
        default: null,
        parseHTML: (el) => {
          const fromAttr = el.getAttribute('width')
          if (fromAttr) return fromAttr
          const styleW = el.style.width?.replace('px', '')
          if (styleW) return styleW
          return null
        },
        renderHTML: (attrs) => {
          if (!attrs.width) return {}
          return { style: `width:${attrs.width}px;height:auto;max-width:100%;` }
        },
      },
    }
  },

  addNodeView() {
    return (props: any) => {
      // Tiptap v2 调用方式: addNodeView() 返回的函数接收一个对象参数
      // { node, editor, view, getPos, decorations, innerDecorations, extension, HTMLAttributes }
      const { node, editor, view, getPos } = props
      return new ResizableImageNodeView(node, editor, getPos, view)
    }
  },
})
