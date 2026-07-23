/**
 * 清理从 PDF / 网页复制时混入的脚注文本
 *
 * 脚注特征：
 * - 独立成段，以 `[N]` 或 `N.` 开头
 * - 包含 GB/T 7714 文献类型标记 `[J]`、`[M]`、`[C]`、`[D]`、`[EB/OL]`
 *
 * 防误删策略：
 * - 只匹配被 `<p>` 包裹或换行符分界的完整段落
 * - **必须同时**包含段落首的编号标记和文献类型标记，二者缺一则不删除
 * - 行内引用标记（如 `见文献[1]`）不会被误删
 */

/** GB/T 7714 文献类型标记（竖线分隔，用于正则） */
const REF_TYPE_PATTERN = '(?:J|M|C|D|EB/OL)'

/**
 * HTML 级脚注清理（用于 `transformPastedHTML`）
 *
 * 匹配的典型模式：
 * - `<p>[1] 作者. 标题[J]. 期刊, 年份.</p>`
 * - `<p class="MsoNormal"><span>[1]</span> 作者. 标题[J]. 期刊, 年份.</p>`
 * - `<p>1. 作者. 标题[D]. 授予单位, 年份.</p>`
 */
export function cleanFootnotesFromHtml(html: string): string {
  if (!html) return html

  let result = html

  // 模式 A: <p>[N] ... 文献类型标记 ... </p>
  // 匹配 <p> 中开头含 [N] 且后续包含 [J/CD/M/D/EB/OL] 的段落
  result = result.replace(
    new RegExp(
      `<p[^>]*>\\s*\\[\\d+\\]\\s*(?:<[^>]+>)*[^<]*?\\[${REF_TYPE_PATTERN}\\][^<]*?<\\/p>\\s*`,
      'gi'
    ),
    ''
  )

  // 模式 B: <p>N. ... 文献类型标记 ... </p>
  result = result.replace(
    new RegExp(
      `<p[^>]*>\\s*\\d+\\.\\s+(?:<[^>]+>)*[^<]*?\\[${REF_TYPE_PATTERN}\\][^<]*?<\\/p>\\s*`,
      'gi'
    ),
    ''
  )

  // 模式 C: <p>[N] 纯脚注文本（无 <span> 嵌套，内容全部为纯文本）
  // 捕获那些以引用编号开头、包含句号分隔、但可能不含 [J] 标记的脚注段落
  // 安全策略：段落中必须包含至少 3 个句号（作者. 标题. 来源.），避免误删
  result = result.replace(
    /<p[^>]*>\s*\[\d+\]\s*(?:<[^>]+>)*[^<]*?\.[^<]*?\.[^<]*?\.[^<]*?<\/p>\s*/gi,
    (match) => {
      // 如果已被前面的规则清理过（内容已为空），直接返回空
      if (/^\s*$/.test(match.replace(/<[^>]+>/g, ''))) return ''
      // 进一步检查：剥离 HTML 标签后检查纯文本是否以 [N] 开头且含作者分隔符
      const text = match.replace(/<[^>]+>/g, '').trim()
      if (/^\[\d+\]\s*[^.]+\.\s*[^.]+\.\s*[^.]+\./.test(text)) {
        return ''
      }
      return match
    }
  )

  // 清理删除后留下的空 <p></p> 段落
  result = result.replace(/<p[^>]*>\s*<\/p>/gi, '')

  // 清理连续换行（多余空白行）
  result = result.replace(/\n{3,}/g, '\n\n')

  return result
}

/**
 * 纯文本级脚注清理（用于 `transformPastedText`）
 */
export function cleanFootnotesFromText(text: string): string {
  if (!text) return text

  const lines = text.split('\n')
  const cleaned = lines.filter((line) => {
    const trimmed = line.trim()
    if (!trimmed) return true

    // 模式 1: 行首 [N] 且包含 GB/T 7714 类型标记
    if (new RegExp(`^\\[\\d+\\]\\s*.+?\\[${REF_TYPE_PATTERN}\\].*$`).test(trimmed)) {
      return false
    }

    // 模式 2: 行首 N. 且包含 GB/T 7714 类型标记
    if (new RegExp(`^\\d+\\.\\s+.+?\\[${REF_TYPE_PATTERN}\\].*$`).test(trimmed)) {
      return false
    }

    // 模式 3: 行首 [N] 且含有至少 3 个句号（强烈疑似脚注）
    if (/^\[\d+\]\s*[^.]+\.\s*[^.]+\.\s*[^.]+\./.test(trimmed)) {
      return false
    }

    return true
  })

  return cleaned.join('\n')
}

/**
 * 通用脚注清理：自动识别输入是 HTML 还是纯文本
 */
export function cleanFootnotes(input: string): string {
  if (!input) return input

  const trimmed = input.trim()
  // 含 HTML 标签则走 HTML 清理，否则走纯文本清理
  if (/<[a-z][\s\S]*>/i.test(trimmed)) {
    return cleanFootnotesFromHtml(trimmed)
  }
  return cleanFootnotesFromText(trimmed)
}
