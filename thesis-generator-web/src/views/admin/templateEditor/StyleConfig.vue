<!-- src/components/admin/TemplateEditor/StyleConfig.vue -->
<template>
  <div class="style-config">
    <el-form label-width="140px" style="max-width:600px;padding-top:12px;">
      <el-divider content-position="left">字体与字号</el-divider>

      <el-form-item label="正文字体">
        <el-select v-model="config.font" style="width:200px;">
          <el-option label="宋体" value="SimSun" />
          <el-option label="仿宋" value="FangSong" />
          <el-option label="黑体" value="SimHei" />
          <el-option label="楷体" value="KaiTi" />
          <el-option label="Times New Roman" value="TimesNewRoman" />
          <el-option label="Calibri" value="Calibri" />
        </el-select>
      </el-form-item>

      <el-form-item label="正文字号">
        <el-select v-model="config.fontSize" style="width:200px;">
          <el-option label="五号 (10.5pt)" value="10.5" />
          <el-option label="小四 (12pt)" value="12" />
          <el-option label="四号 (14pt)" value="14" />
          <el-option label="三号 (16pt)" value="16" />
        </el-select>
      </el-form-item>

      <el-form-item label="标题字体">
        <el-select v-model="config.headingFont" style="width:200px;">
          <el-option label="黑体" value="SimHei" />
          <el-option label="宋体" value="SimSun" />
          <el-option label="Times New Roman" value="TimesNewRoman" />
        </el-select>
      </el-form-item>

      <el-divider content-position="left">段落与间距</el-divider>

      <el-form-item label="行距">
        <el-select v-model="config.lineHeight" style="width:200px;">
          <el-option label="单倍 (1.0)" value="1.0" />
          <el-option label="1.25 倍" value="1.25" />
          <el-option label="1.5 倍" value="1.5" />
          <el-option label="2 倍" value="2" />
        </el-select>
      </el-form-item>

      <el-form-item label="首行缩进">
        <el-select v-model="config.indent" style="width:200px;">
          <el-option label="2 字符" value="2" />
          <el-option label="无缩进" value="0" />
          <el-option label="4 字符" value="4" />
        </el-select>
      </el-form-item>

      <el-form-item label="段落间距">
        <el-input-number v-model="config.paragraphSpacing" :min="0" :max="20" style="width:150px;" />
        <span style="margin-left:8px;color:#909399;">磅</span>
      </el-form-item>

      <el-divider content-position="left">页面设置</el-divider>

      <el-form-item label="上边距">
        <el-input-number v-model="config.marginTop" :min="1" :max="5" :step="0.1" style="width:120px;" />
        <span style="margin-left:8px;color:#909399;">cm</span>
      </el-form-item>

      <el-form-item label="下边距">
        <el-input-number v-model="config.marginBottom" :min="1" :max="5" :step="0.1" style="width:120px;" />
        <span style="margin-left:8px;color:#909399;">cm</span>
      </el-form-item>

      <el-form-item label="左边距">
        <el-input-number v-model="config.marginLeft" :min="1" :max="5" :step="0.1" style="width:120px;" />
        <span style="margin-left:8px;color:#909399;">cm</span>
      </el-form-item>

      <el-form-item label="右边距">
        <el-input-number v-model="config.marginRight" :min="1" :max="5" :step="0.1" style="width:120px;" />
        <span style="margin-left:8px;color:#909399;">cm</span>
      </el-form-item>

      <el-divider content-position="left">页眉页脚</el-divider>

      <el-form-item label="页眉内容">
        <el-input v-model="config.headerText" placeholder="如：XX大学毕业论文" style="width:300px;" />
      </el-form-item>

      <el-form-item label="页眉字号">
        <el-select v-model="config.headerFontSize" style="width:150px;">
          <el-option label="五号 (10.5pt)" value="10.5" />
          <el-option label="小五 (9pt)" value="9" />
        </el-select>
      </el-form-item>

      <el-form-item label="页码位置">
        <el-radio-group v-model="config.pageNumberPosition">
          <el-radio label="bottomCenter">底部居中</el-radio>
          <el-radio label="bottomRight">底部靠右</el-radio>
          <el-radio label="topCenter">顶部居中</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{
  modelValue?: any
}>()

const config = reactive({
  font: 'SimSun',
  fontSize: '12',
  headingFont: 'SimHei',
  lineHeight: '1.5',
  indent: '2',
  paragraphSpacing: 0,
  marginTop: 2.54,
  marginBottom: 2.54,
  marginLeft: 3.17,
  marginRight: 3.17,
  headerText: '',
  headerFontSize: '10.5',
  pageNumberPosition: 'bottomCenter',
})

// 初始化
if (props.modelValue) {
  Object.assign(config, props.modelValue)
}

watch(config, () => {
  emit('update:modelValue', { ...config })
}, { deep: true })

function getConfig() {
  return { ...config }
}

defineExpose({ getConfig })

const emit = defineEmits<{
  (e: 'update:modelValue', value: any): void
}>()
</script>

<style scoped lang="scss">
.style-config {
  padding: 0 20px;
}
</style>
