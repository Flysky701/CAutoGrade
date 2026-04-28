export function formatDate(date: string | Date): string {
  const d = new Date(date)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function roleLabel(role: string): string {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[role] || role
}

export function statusTag(status: string) {
  const map: Record<string, { type: string; label: string }> = {
    DRAFT: { type: 'info', label: '草稿' },
    PUBLISHED: { type: 'success', label: '已发布' },
    EXPIRED: { type: 'warning', label: '已截止' },
    ARCHIVED: { type: 'info', label: '已归档' },
    ACTIVE: { type: 'success', label: '进行中' },
    PENDING: { type: 'warning', label: '排队中' },
    PROCESSING: { type: '', label: '批阅中' },
    DONE: { type: 'success', label: '已完成' },
    FAILED: { type: 'danger', label: '失败' },
  }
  return map[status] || { type: 'info', label: status }
}

export function difficultyStars(level: number): string {
  return '★'.repeat(level) + '☆'.repeat(5 - level)
}
