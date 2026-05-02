import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/request'

export const useAssignmentStore = defineStore('assignment', () => {
  const list = ref<any[]>([])
  const current = ref<any>(null)

  async function fetchByCourse(courseId: number) {
    try {
      const res = await api.get(`/assignments/course/${courseId}`) as any
      list.value = res?.data || []
    } catch {}
  }

  async function fetchAll() {
    try {
      const res = await api.get('/assignments') as any
      return res?.data || []
    } catch {
      return []
    }
  }

  async function create(data: any) {
    return api.post('/assignments', data)
  }

  async function publish(id: number) {
    return api.put(`/assignments/${id}/publish`)
  }

  return { list, current, fetchByCourse, fetchAll, create, publish }
})
