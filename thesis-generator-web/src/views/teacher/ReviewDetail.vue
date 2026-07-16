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
      </div>

      <!-- 论文信息 -->
      <div class="paper-info">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="学生">{{ paper?.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ paper?.studentId }}</el-descriptions-item>
          <el-descriptions-item label="课程">{{ paper?.course }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ paper?.submittedAt }}</el-descriptions-item>
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
          <div class="preview-content" v-loading="loading">
            <!-- 论文内容占位 -->
            <div class="paper-content">
              <h2>{{ paper?.title }}</h2>
              <p style="color:#909399;">摘要：本文研究了...</p>
              <h3>1. 引言</h3>
              <p>随着信息技术的发展...</p>
              <h3>2. 相关工作</h3>
              <p>在已有研究基础上...</p>
              <!-- 更多内容 -->
            </div>
          </div>
        </div>

        <!-- 右侧：批注侧边栏 -->
        <div class="annotation-sidebar" v-show="showSidebar">
          <div class="sidebar-header">
            <span>💬 批注</span>
            <el-button size="small" type="primary" @click="addAnnotation">+ 添加批注</el-button>
          </div>
          <div class="annotation-list">
            <div v-for="ann in annotations" :key="ann.id" class="annotation-item">
              <div class="ann-content">{{ ann.content }}</div>
              <div class="ann-meta">
                <span>{{ ann.author }}</span>
                <span>{{ new Date(ann.createdAt).toLocaleString('zh-CN') }}</span>
              </div>
              <div class="ann-actions">
                <el-button size="small" link @click="editAnnotation(ann)">编辑</el-button>
                <el-button size="small" link type="danger" @click="deleteAnnotation(ann.id)">删除</el-button>
              </div>
            </div>
            <div v-if="!annotations.length" class="empty-annotations">
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
          <el-button @click="saveDraft">暂存</el-button>
          <el-button type="warning" @click="handleReturn">退回</el-button>
          <el-button type="success" @click="handleApprove">通过</el-button>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const showSidebar = ref(true)

const paper = ref<any>(null)
const comment = ref('')
const score = ref<number>(85)
const grade = ref('良')
const annotations = ref<any[]>([])

async function fetchData() {
  const id = route.params.id
  loading.value = true
  try {
    // TODO: 调用真实 API
    // const res = await reviewApi.getDetail(Number(id))
    // paper.value = res.data.paper
    // annotations.value = res.data.annotations

    await new Promise((r) => setTimeout(r, 500))
    paper.value = {
      id: Number(id),
      title: '基于深度学习的图像分割研究',
      studentName: '张三',
      studentId: '2024001',
      course: '软件工程',
      status: 'SUBMITTED',
      submittedAt: '2026-07-15 14:30',
    }
    annotations.value = [
      {
        id: 1,
        content: '实验数据来源需要补充引用',
        author: '教师 李四',
        createdAt: '2026-07-15T15:00:00',
      },
      {
        id: 2,
        content: '图表标题格式应为"表1-1 实验对比结果"',
        author: '教师 李四',
        createdAt: '2026-07-15T15:30:00',
      },
    ]
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function toggleSidebar() {
  showSidebar.value = !showSidebar.value
}

function addAnnotation() {
  ElMessage.info('选中论文文字后点击即可添加批注（开发中）')
}

function editAnnotation(ann: any) {
  ElMessage.info('编辑批注功能开发中')
}

function deleteAnnotation(id: number) {
  ElMessageBox.confirm('确定删除此批注吗？', '提示', { type: 'warning' })
    .then(() => {
      annotations.value = annotations.value.filter((a) => a.id !== id)
      ElMessage.success('已删除')
    })
    .catch(() => {})
}

function saveDraft() {
  // TODO: 保存评语和评分
  ElMessage.success('已暂存')
}

function handleReturn() {
  if (!comment.value) {
    ElMessage.warning('请填写退回意见')
    return
  }
  ElMessageBox.confirm('确定退回该论文吗？学生将收到修改通知。', '确认退回', { type: 'warning' })
    .then(() => {
      ElMessage.success('已退回')
      router.back()
    })
    .catch(() => {})
}

function handleApprove() {
  if (!comment.value) {
    ElMessage.warning('请填写评语')
    return
  }
  if (!score.value) {
    ElMessage.warning('请填写分数')
    return
  }
  ElMessageBox.confirm('确定通过该论文吗？', '确认通过', { type: 'info' })
    .then(() => {
      ElMessage.success('已通过')
      router.back()
    })
    .catch(() => {})
}

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
  }
}

.paper-content {
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
