import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/request'

export const useGradingStore = defineStore('grading', () => {
  const current = ref<any>(null)
  const loading = ref(false)

  async function fetchResult(submissionId: number) {
    loading.value = true
    try {
      const res = await api.get(`/gradings/submission/${submissionId}`) as any
      current.value = res?.data
    } finally {
      loading.value = false
    }
  }

  async function fetchByAssignment(assignmentId: number) {
    try {
      const res = await api.get(`/gradings/assignment/${assignmentId}`) as any
      return res?.data || []
    } catch {
      return []
    }
  }

  return { current, loading, fetchResult, fetchByAssignment }
})
