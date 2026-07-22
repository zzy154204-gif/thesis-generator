import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useEditorStore = defineStore('editor', () => {
  const currentSectionId = ref<number | null>(null)
  const isDirty = ref(false)
  const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')
  const lastSaveTime = ref('')

  function setCurrentSection(id: number) {
    currentSectionId.value = id
  }

  function markDirty() {
    isDirty.value = true
    saveStatus.value = 'unsaved'
  }

  function markSaved() {
    isDirty.value = false
    saveStatus.value = 'saved'
    lastSaveTime.value = new Date().toLocaleTimeString('zh-CN', {
      hour: '2-digit', minute: '2-digit',
    })
  }

  function markSaving() {
    saveStatus.value = 'saving'
  }

  function reset() {
    currentSectionId.value = null
    isDirty.value = false
    saveStatus.value = 'saved'
    lastSaveTime.value = ''
  }

  return {
    currentSectionId, isDirty, saveStatus, lastSaveTime,
    setCurrentSection, markDirty, markSaved, markSaving, reset,
  }
})
