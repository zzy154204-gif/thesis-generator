import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Thesis, ThesisSection } from '@/types/api'
import { getPapers, getPaper } from '@/api/paper'

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

  async function fetchSections(_paperId: number) {
    // 后端 SectionController 尚未实现（B 负责），暂时使用默认章节结构
    // 等 B 实现后改为: const res = await getSections(paperId); sections.value = res.data || []
    sections.value = [
      { id: 1, title: '第一章 引言', content: '', children: [] },
      { id: 2, title: '第二章 文献综述', content: '', children: [] },
      { id: 3, title: '第三章 研究方法', content: '', children: [] },
      { id: 4, title: '第四章 结果与讨论', content: '', children: [] },
      { id: 5, title: '第五章 结论', content: '', children: [] },
      { id: 6, title: '参考文献', content: '', children: [] },
    ] as any[]
  }

  function clearCurrent() {
    currentPaper.value = null
    sections.value = []
  }

  return { paperList, currentPaper, sections, fetchPapers, fetchPaper, fetchSections, clearCurrent }
})
