import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Thesis, ThesisSection } from '@/types/api'
import { getPapers, getPaper } from '@/api/paper'
import { getSections } from '@/api/section'

export const usePaperStore = defineStore('paper', () => {
  const paperList = ref<Thesis[]>([])
  const currentPaper = ref<Thesis | null>(null)
  const sections = ref<ThesisSection[]>([])

  async function fetchPapers(keyword?: string, sortBy?: string) {
    const res = await getPapers({ keyword, sortBy })
    paperList.value = res.data || []
  }

  async function fetchPaper(id: number) {
    const res = await getPaper(id)
    currentPaper.value = res.data
  }

  async function fetchSections(paperId: number) {
    try {
      const res = await getSections(paperId)
      sections.value = res.data || []
    } catch {
      // 后端 SectionController 尚未实现（B 负责），暂时使用空列表
      sections.value = []
    }
  }

  function clearCurrent() {
    currentPaper.value = null
    sections.value = []
  }

  return { paperList, currentPaper, sections, fetchPapers, fetchPaper, fetchSections, clearCurrent }
})
