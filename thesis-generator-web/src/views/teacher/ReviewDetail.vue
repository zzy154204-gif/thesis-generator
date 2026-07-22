<!-- src/views/teacher/ReviewDetail.vue -->
<template>
  <DefaultLayout>
    <div class="review-detail">
      <!-- 顶栏 -->
      <div class="detail-header">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <span class="title">{{ paper?.title || '加载中...' }}</span>
        <el-tag :type="paper?.status === 'SUBMITTED' ? 'warning' : 'primary'" size="small">
          {{ paper?.status === 'SUBMITTED' ? '待批阅' : '批阅中' }}
        </el-tag>
        <el-tag v-if="paper?.version" type="info" size="small">V{{ paper.version }}</el-tag>
      </div>

      <!-- 论文信息 -->
      <div class="paper-info">
        <el-descriptions :column="4" border size="small">
          <!--el-descriptions-item label="学生">{{ paper?.studentName || '未知' }}</el-descriptions-item-->
          <el-descriptions-item label="学号">{{ paper?.studentId || '未知' }}</el-descriptions-item>
          <!--el-descriptions-item label="课程">{{ paper?.course || '未知' }}</el-descriptions-item-->
          <el-descriptions-item label="提交时间">{{ formatDate(paper?.submittedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 主体 -->
      <div class="review-body">
        <!-- 左侧：论文预览 -->
        <div class="preview-area">
          <div class="preview-header">
            <span>📄 论文预览</span>
            <el-button size="small" @click="toggleSidebar">
              {{ showSidebar ? '隐藏批注' : '显示批注' }}
            </el-button>
          </div>
          <div
            class="preview-content"
            v-loading="loading"
            @mouseup="handleTextSelection"
          >
            <!--div class="paper-content" ref="paperContentRef">
              <h2>{{ paper?.title }}</h2>
              <p><strong>摘要：</strong>{{ paper?.abstract || '暂无摘要' }}</p>
              <div v-html="paper?.content || '<p>论文内容加载中...</p>'"></div>
            </div-->

            <!-- 浮动批注输入框 -->
            <div
              v-if="showAnnotationInput"
              class="floating-annotation-input"
              :style="{
                left: annotationInputPosition.x + 'px',
                top: annotationInputPosition.y + 'px',
              }"
              ref="annotationInputRef"
            >
              <el-input
                v-model="annotationContent"
                type="textarea"
                :rows="3"
                placeholder="请输入批注内容..."
                maxlength="200"
                show-word-limit
                ref="annotationTextareaRef"
                @keydown.esc="cancelAnnotation"
              />
              <div class="floating-actions">
                <el-button size="small" @click="cancelAnnotation">取消</el-button>
                <el-button
                  size="small"
                  type="primary"
                  :loading="annotationSaving"
                  @click="saveAnnotation"
                >
                  确定
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：批注侧边栏 -->
        <div class="annotation-sidebar" v-show="showSidebar">
          <div class="sidebar-header">
            <span>💬 批注 ({{ annotations.length }})</span>
            <el-button size="small" type="primary" @click="startAddAnnotation">
              + 添加批注
            </el-button>
          </div>
          <div class="annotation-list" v-loading="annotationLoading">
            <div v-for="ann in annotations" :key="ann.id" class="annotation-item">
              <div class="ann-content">{{ ann.content }}</div>
              <div class="ann-meta">
                <span>教师 ID: {{ ann.teacherId }}</span>
                <span>{{ formatDate(ann.createdAt) }}</span>
              </div>
              <div class="ann-actions">
                <el-button size="small" link @click="editAnnotation(ann)">编辑</el-button>
                <el-button size="small" link type="danger" @click="handleDeleteAnnotation(ann.id)">删除</el-button>
              </div>
            </div>
            <div v-if="!annotations.length && !annotationLoading" class="empty-annotations">
              <el-empty description="暂无批注" :image-size="60" />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：评语 + 评分 + 操作 -->
      <div class="review-footer">
        <div class="footer-left">
          <el-form label-width="80px" style="flex:1;">
            <el-form-item label="评语">
              <el-input
                v-model="comment"
                type="textarea"
                :rows="3"
                placeholder="请输入整体评语..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>
          <div class="score-area">
            <el-form label-width="60px">
              <el-form-item label="分数">
                <el-input-number
                  v-model="score"
                  :min="0"
                  :max="100"
                  :step="0.5"
                  style="width:120px;"
                />
              </el-form-item>
              <el-form-item label="等级">
                <el-select v-model="grade" style="width:100px;">
                  <el-option label="优" value="优" />
                  <el-option label="良" value="良" />
                  <el-option label="中" value="中" />
                  <el-option label="及格" value="及格" />
                  <el-option label="不及格" value="不及格" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="footer-right">
          <!-- 暂存按钮暂时隐藏，后端暂无 /draft 接口 -->
          <!-- <el-button :loading="draftLoading" @click="saveDraft">暂存</el-button> -->
          <el-button type="warning" :loading="returnLoading" @click="handleReturn">退回</el-button>
          <el-button type="success" :loading="approveLoading" @click="handleApprove">通过</el-button>
        </div>
      </div>
    </div>

    <!-- 编辑批注弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑批注"
      width="500px"
      destroy-on-close
    >
      <el-input
        v-model="editContent"
        type="textarea"
        :rows="4"
        placeholder="请输入批注内容"
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="confirmEdit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import {
  getReviewHistory,
  getAnnotations,
  addAnnotation,
  updateAnnotation,
  deleteAnnotation,
  returnPaper,
  approvePaper,
  type ReviewRecord,
  type Annotation,
} from '@/api/review'
import { getPaper } from '@/api/paper'

const router = useRouter()
const route = useRoute()
const paperId = ref<number>(0)

// ===== 状态 =====
const loading = ref(false)
const annotationLoading = ref(false)
const draftLoading = ref(false)
const returnLoading = ref(false)
const approveLoading = ref(false)
const showSidebar = ref(true)

// ===== 编辑批注状态 =====
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editContent = ref('')
const editingAnnotationId = ref<number | null>(null)

// ===== 浮动批注状态 =====
const showAnnotationInput = ref(false)
const annotationSaving = ref(false)
const annotationContent = ref('')
const annotationInputPosition = ref({ x: 0, y: 0 })
const paperContentRef = ref<HTMLElement | null>(null)
const annotationInputRef = ref<HTMLElement | null>(null)
const annotationTextareaRef = ref<any>(null)

const selectedTextInfo = ref<{
  text: string
  startOffset: number
  endOffset: number
  sectionId: number
} | null>(null)

// ===== 数据 =====
const paper = ref<{
  id: number
  title: string
  studentName: string
  studentId: string
  course: string
  status: string
  version: number
  abstract?: string
  content?: string
  submittedAt: string
} | null>(null)

const annotations = ref<Annotation[]>([])
const comment = ref('')
const score = ref<number>(85)
const grade = ref('良')

// ===== 格式化日期 =====
function formatDate(date: string | undefined | null) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

async function fetchData() {
  const id = route.params.id
  if (!id) {
    ElMessage.error('缺少论文ID')
    return
  }
  paperId.value = Number(id)
  loading.value = true

  try {
    // 1. 获取论文详情（返回 Thesis 类型）
    const paperRes = await getPaper(paperId.value)
    const thesis = paperRes.data  // 类型：Thesis

    // 2. 获取批阅历史
    const historyRes = await getReviewHistory(paperId.value)
    const records = historyRes.data || []
    const latestRecord = records.length > 0 ? records[0] : null

    // 3. 获取批注列表
    const annRes = await getAnnotations(paperId.value)
    annotations.value = annRes.data || []

    // 4. 构建 paper 对象（使用 Thesis 类型中的字段）
    paper.value = {
      id: thesis.id,
      title: thesis.title || `论文 #${thesis.id}`,
      status: thesis.status || 'SUBMITTED',
      studentId: thesis.studentId?.toString() || '未知',
      //studentName: `学生 ${thesis.studentId}`,  // 临时，等用户接口
      //course: `课程 #${thesis.collegeId || '未知'}`,  // 临时，等课程接口
      version: records.length,
      submittedAt: thesis.updatedAt || latestRecord?.createdAt || new Date().toISOString(),
      // abstract 和 content 不在 Thesis 类型中，暂时用默认值
      //abstract: '暂无摘要',
      //content: '<p>论文正文加载中...</p>',
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 切换侧边栏 =====
function toggleSidebar() {
  showSidebar.value = !showSidebar.value
}

// ===== 开始添加批注（点击按钮） =====
function startAddAnnotation() {
  ElMessage.info('请先在论文预览区选中文字，再添加批注')
}

// ===== 处理文本选择 =====
function handleTextSelection() {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed || !selection.toString().trim()) {
    if (showAnnotationInput.value) {
      showAnnotationInput.value = false
    }
    return
  }

  const selectedText = selection.toString().trim()
  if (!selectedText) return

  const range = selection.getRangeAt(0)
  const preSelectionRange = range.cloneRange()
  const contentEl = paperContentRef.value
  if (!contentEl) return

  preSelectionRange.selectNodeContents(contentEl)
  preSelectionRange.setEnd(range.startContainer, range.startOffset)
  const startOffset = preSelectionRange.toString().length

  const currentSectionId = 1

  selectedTextInfo.value = {
    text: selectedText,
    startOffset: startOffset,
    endOffset: startOffset + selectedText.length,
    sectionId: currentSectionId,
  }

  const rect = range.getBoundingClientRect()
  const containerRect = contentEl.getBoundingClientRect()

  annotationInputPosition.value = {
    x: rect.left - containerRect.left,
    y: rect.bottom - containerRect.top + 8,
  }

  annotationContent.value = ''
  showAnnotationInput.value = true

  nextTick(() => {
    annotationTextareaRef.value?.focus()
  })
}

// ===== 取消批注 =====
function cancelAnnotation() {
  showAnnotationInput.value = false
  annotationContent.value = ''
  selectedTextInfo.value = null
  window.getSelection()?.removeAllRanges()
}

// ===== 保存批注 =====
async function saveAnnotation() {
  if (!annotationContent.value.trim()) {
    ElMessage.warning('请输入批注内容')
    return
  }
  if (!selectedTextInfo.value) {
    ElMessage.warning('请先选中文字')
    return
  }

  annotationSaving.value = true
  try {
    const { text, startOffset, sectionId } = selectedTextInfo.value

    await addAnnotation({
      thesisId: paperId.value,
      sectionId: sectionId,
      startOffset: startOffset,
      textLength: text.length,
      selectedText: text,
      content: annotationContent.value.trim(),
    })

    ElMessage.success('批注已添加')

    const annRes = await getAnnotations(paperId.value)
    annotations.value = annRes.data || []

    cancelAnnotation()
  } catch {
    ElMessage.error('添加批注失败')
  } finally {
    annotationSaving.value = false
  }
}

// ===== 编辑批注 =====
function editAnnotation(ann: Annotation) {
  editingAnnotationId.value = ann.id
  editContent.value = ann.content
  editDialogVisible.value = true
}

// ===== 确认编辑批注 =====
async function confirmEdit() {
  if (!editContent.value.trim()) {
    ElMessage.warning('批注内容不能为空')
    return
  }
  if (editingAnnotationId.value === null) return

  const id = editingAnnotationId.value
  editLoading.value = true
  try {
    await updateAnnotation(id, editContent.value.trim())

    const index = annotations.value.findIndex((a) => a.id === id)
    if (index !== -1) {
      annotations.value[index].content = editContent.value.trim()
    }
    ElMessage.success('批注已更新')
    editDialogVisible.value = false
  } catch {
    ElMessage.error('更新失败')
  } finally {
    editLoading.value = false
  }
}

// ===== 删除批注 =====
function handleDeleteAnnotation(annotationId: number) {
  ElMessageBox.confirm('确定删除此批注吗？', '提示', { type: 'warning' })
    .then(async () => {
      annotationLoading.value = true
      try {
        await deleteAnnotation(annotationId)
        annotations.value = annotations.value.filter((a) => a.id !== annotationId)
        ElMessage.success('已删除')
      } catch {
        ElMessage.error('删除失败')
      } finally {
        annotationLoading.value = false
      }
    })
    .catch(() => {})
}

// ===== 退回 =====
function handleReturn() {
  if (!comment.value.trim()) {
    ElMessage.warning('请填写退回意见')
    return
  }
  ElMessageBox.confirm('确定退回该论文吗？学生将收到修改通知。', '确认退回', { type: 'warning' })
    .then(async () => {
      returnLoading.value = true
      try {
        await returnPaper(paperId.value, { comment: comment.value })
        ElMessage.success('已退回')
        router.back()
      } catch {
        ElMessage.error('操作失败')
      } finally {
        returnLoading.value = false
      }
    })
    .catch(() => {})
}

// ===== 通过 =====
function handleApprove() {
  if (!comment.value.trim()) {
    ElMessage.warning('请填写评语')
    return
  }
  if (!score.value) {
    ElMessage.warning('请填写分数')
    return
  }

  ElMessageBox.confirm(
    `确定通过该论文吗？得分：${score.value}分，等级：${grade.value}`,
    '确认通过',
    { type: 'info' }
  )
    .then(async () => {
      approveLoading.value = true
      try {
        await approvePaper(paperId.value, {
          comment: comment.value,
          score: score.value,
          grade: grade.value,
        })
        ElMessage.success('已通过')
        router.back()
      } catch {
        ElMessage.error('操作失败')
      } finally {
        approveLoading.value = false
      }
    })
    .catch(() => {})
}

// ===== 生命周期 =====
onMounted(fetchData)
</script>

<style scoped lang="scss">
.review-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  .title {
    font-size: 16px;
    font-weight: 600;
    flex: 1;
  }
}

.paper-info {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
}

.review-body {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 400px;
}

.preview-area {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .preview-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    position: relative;
  }
}

.paper-content {
  position: relative;
  max-width: 700px;
  margin: 0 auto;
  font-size: 14px;
  line-height: 1.8;

  h2 {
    text-align: center;
    font-size: 20px;
    margin-bottom: 20px;
  }
  h3 {
    font-size: 17px;
    margin: 16px 0 10px;
  }
  p {
    margin: 8px 0;
  }
}

.floating-annotation-input {
  position: absolute;
  z-index: 1000;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  width: 320px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 280px;

  .floating-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 8px;
  }
}

.annotation-sidebar {
  width: 320px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .annotation-list {
    flex: 1;
    overflow-y: auto;
    padding: 12px 16px;
  }
}

.annotation-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;

  .ann-content {
    font-size: 14px;
    margin-bottom: 8px;
  }
  .ann-meta {
    font-size: 12px;
    color: #909399;
    display: flex;
    gap: 12px;
  }
  .ann-actions {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}

.empty-annotations {
  padding: 40px 0;
}

.review-footer {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .footer-left {
    display: flex;
    gap: 20px;
    flex: 1;
  }

  .footer-right {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
    border-top: 1px solid #ebeef5;
    padding-top: 12px;
  }

  .score-area {
    flex-shrink: 0;
  }
}
</style>
