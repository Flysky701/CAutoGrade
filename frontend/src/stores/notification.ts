import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/request'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const list = ref<any[]>([])

  async function fetchUnread() {
    try {
      const res = await api.get('/notifications/unread-count') as any
      unreadCount.value = res?.data || 0
    } catch {}
  }

  async function fetchList() {
    try {
      const res = await api.get('/notifications', { params: { page: 1, size: 50 } }) as any
      list.value = res?.data?.records || res?.data || []
    } catch {}
  }

  async function markAllRead() {
    try {
      await api.put('/notifications/read-all')
      unreadCount.value = 0
    } catch {}
  }

  return { unreadCount, list, fetchUnread, fetchList, markAllRead }
})
