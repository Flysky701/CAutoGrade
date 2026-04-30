import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/notification'

// Mock axios
vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}))

import api from '@/api/request'

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with zero unread', () => {
    const store = useNotificationStore()
    expect(store.unreadCount).toBe(0)
    expect(store.list).toEqual([])
  })

  it('fetchUnread should update unreadCount', async () => {
    const mockGet = vi.mocked(api.get)
    mockGet.mockResolvedValue({ data: 5 })

    const store = useNotificationStore()
    await store.fetchUnread()

    expect(store.unreadCount).toBe(5)
    expect(mockGet).toHaveBeenCalledWith('/notifications/unread-count')
  })

  it('fetchUnread should not throw on error', async () => {
    const mockGet = vi.mocked(api.get)
    mockGet.mockRejectedValue(new Error('Network error'))

    const store = useNotificationStore()
    await store.fetchUnread()

    // Should still be 0 (no change on error)
    expect(store.unreadCount).toBe(0)
  })

  it('fetchList should update list', async () => {
    const mockGet = vi.mocked(api.get)
    mockGet.mockResolvedValue({
      data: {
        records: [
          { id: 1, title: '通知1' },
          { id: 2, title: '通知2' },
        ],
      },
    })

    const store = useNotificationStore()
    await store.fetchList()

    expect(store.list).toHaveLength(2)
    expect(store.list[0].title).toBe('通知1')
    expect(mockGet).toHaveBeenCalledWith('/notifications', { params: { page: 1, size: 50 } })
  })

  it('markAllRead should reset unreadCount', async () => {
    const mockPut = vi.mocked(api.put)
    mockPut.mockResolvedValue({})

    const store = useNotificationStore()
    store.unreadCount = 10
    await store.markAllRead()

    expect(store.unreadCount).toBe(0)
    expect(mockPut).toHaveBeenCalledWith('/notifications/read-all')
  })
})
