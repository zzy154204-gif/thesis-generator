<template>
  <DefaultLayout>
    <div class="page">
      <div class="header">
        <el-button text :icon="ArrowLeft" @click="router.push('/papers')">返回</el-button>
        <h2>导入论文 (.docx)</h2>
      </div>

      <el-card class="form-card">
        <el-form :model="form" label-width="100px">
          <!-- 论文标题 -->
          <el-form-item label="论文标题" required>
            <el-input
              v-model="form.title"
              placeholder="请输入论文标题"
              maxlength="200"
              :disabled="uploading"
            />
          </el-form-item>

          <!-- 关联模板（可选） -->
          <el-form-item label="关联模板">
            <el-select
              v-model="form.templateVersionId"
              placeholder="选择模板（可选）· 按模板章节结构导入"
              clearable
              filterable
              style="width:100%"
              :disabled="uploading"
            >
              <el-option
                v-for="t in templates"
                :key="t.templateVersionId"
                :label="t.name"
                :value="t.templateVersionId"
              >
                <span>{{ t.name }}</span>
                <span class="opt-desc">{{ templateDesc(t) }}</span>
              </el-option>
            </el-select>
            <div class="tip">选择模板后，系统会按模板章节结构组织导入内容；不选则由系统自动识别章节</div>
          </el-form-item>

          <!-- 文件上传拖拽区 -->
          <el-form-item label="选择文件" required>
            <div
              class="upload-zone"
              :class="{ 'is-dragover': dragOver, 'is-uploading': uploading, 'is-success': uploadSuccess }"
              @dragover.prevent="dragOver = true"
              @dragleave.prevent="dragOver = false"
              @drop.prevent="onDrop"
              @click="fileInputRef?.click()"
            >
              <input
                ref="fileInputRef"
                type="file"
                accept=".docx"
                style="display:none"
                @change="onFileChange"
              />

              <!-- 已选择文件 -->
              <template v-if="selectedFile">
                <el-icon class="file-icon" :size="48"><Document /></el-icon>
                <div class="file-name">{{ selectedFile.name }}</div>
                <div class="file-size">{{ formatSize(selectedFile.size) }}</div>
                <el-button
                  v-if="!uploading"
                  text
                  type="danger"
                  size="small"
                  @click.stop="selectedFile = null"
                  style="margin-top:4px"
                >
                  移除
                </el-button>
              </template>

              <!-- 未选择文件 - 拖拽提示 -->
              <template v-else>
                <el-icon class="upload-icon" :size="48"><UploadFilled /></el-icon>
                <div class="upload-text">
                  <span style="color:var(--el-color-primary)">点击选择</span> 或将 .docx 文件拖拽到此处
                </div>
                <div class="upload-hint">仅支持 Word .docx 格式</div>
              </template>
            </div>
          </el-form-item>

          <!-- 提交按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="uploading"
              :disabled="!canSubmit"
              @click="handleImport"
            >
              {{ uploading ? '正在解析...' : '开始导入' }}
            </el-button>
            <el-button size="large" :disabled="uploading" @click="router.push('/papers')">取消</el-button>
          </el-form-item>

          <!-- 进度提示 -->
          <el-form-item v-if="uploading">
            <div class="progress-tip">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在上传并解析文档，大文件可能需要几秒钟...</span>
            </div>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Document, UploadFilled, Loading } from '@element-plus/icons-vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { importDocx } from '@/api/paper'
import { getAvailableTemplates, getTemplateVersions } from '@/api/template'

const router = useRouter()

const fileInputRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)
const uploading = ref(false)
const uploadSuccess = ref(false)
const selectedFile = ref<File | null>(null)
const templates = ref<any[]>([])

const form = ref({
  title: '未命名论文',
  templateVersionId: undefined as number | undefined,
})

const canSubmit = computed(() => {
  return form.value.title.trim() && selectedFile.value && !uploading.value
})

function templateDesc(t: any) {
  return t.type === 'GRADUATION' ? '毕业论文' : t.type === 'COURSE' ? '课程论文' : '项目报告'
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function validateDocx(file: File): boolean {
  if (!file.name.toLowerCase().endsWith('.docx')) {
    ElMessage.warning('仅支持 .docx 格式的 Word 文档')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小超出限制（最大 50MB）')
    return false
  }
  return true
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (validateDocx(file)) {
      selectedFile.value = file
      // 如果标题还没改，用文件名作为默认标题
      if (form.value.title === '未命名论文') {
        form.value.title = file.name.replace(/\.docx$/i, '')
      }
    }
  }
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    const file = files[0]
    if (validateDocx(file)) {
      selectedFile.value = file
      if (form.value.title === '未命名论文') {
        form.value.title = file.name.replace(/\.docx$/i, '')
      }
    }
  }
  // 重置 input 以便再次选择同一文件
  if (fileInputRef.value) fileInputRef.value.value = ''
}

async function handleImport() {
  if (!selectedFile.value || !form.value.title.trim()) return

  uploading.value = true
  uploadSuccess.value = false
  try {
    const res = await importDocx(
      selectedFile.value,
      form.value.title.trim(),
      form.value.templateVersionId,
    )
    uploadSuccess.value = true
    ElMessage.success(`论文「${res.data.title}」导入成功，已自动解析章节结构`)
    router.push(`/editor/${res.data.id}`)
  } catch (e: any) {
    // 错误已在 request 拦截器中处理
  } finally {
    uploading.value = false
  }
}

// 加载可选模板列表
onMounted(async () => {
  try {
    const tmplRes = await getAvailableTemplates()
    const tmpls = tmplRes.data || []
    const full = await Promise.all(tmpls.map(async (t: any) => {
      try {
        const verRes = await getTemplateVersions(t.id)
        const active = (verRes.data || []).find((v: any) => v.isCurrent)
        return { ...t, templateVersionId: active?.id }
      } catch {
        return { ...t, templateVersionId: undefined }
      }
    }))
    templates.value = full.filter((t: any) => t.templateVersionId)
  } catch { /* ignore */ }
})
</script>

<style scoped lang="scss">
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  h2 { font-size: 20px; font-weight: 600; }
}

.form-card { max-width: 700px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; line-height: 1.4; }
.opt-desc { font-size: 12px; color: var(--el-text-color-secondary); margin-left: 8px; }

.upload-zone {
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;
  background: var(--el-fill-color-lighter);

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  &.is-dragover {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    transform: scale(1.01);
  }

  &.is-uploading {
    pointer-events: none;
    opacity: 0.7;
  }

  &.is-success {
    border-color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }

  .upload-icon, .file-icon {
    margin-bottom: 12px;
    color: var(--el-text-color-secondary);
  }

  .upload-text {
    font-size: 15px;
    color: var(--el-text-color-regular);
    margin-bottom: 8px;
  }

  .upload-hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .file-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin-bottom: 4px;
  }

  .file-size {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.progress-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--el-color-primary);
}
</style>
