import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// Mock window.location
const mockHref = vi.fn()
Object.defineProperty(window, 'location', {
  value: { href: '' },
  writable: true,
})

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should initialize with empty values when localStorage is empty', () => {
    const store = useAuthStore()
    expect(store.token).toBe('')
    expect(store.role).toBe('')
    expect(store.username).toBe('')
  })

  it('should initialize from localStorage', () => {
    localStorage.setItem('token', 'jwt.token.here')
    localStorage.setItem('role', 'STUDENT')
    localStorage.setItem('username', 'testuser')
    localStorage.setItem('code', 'S2026001')

    setActivePinia(createPinia())
    const store = useAuthStore()

    expect(store.token).toBe('jwt.token.here')
    expect(store.role).toBe('STUDENT')
    expect(store.username).toBe('testuser')
    expect(store.code).toBe('S2026001')
  })

  it('setAuth should persist to localStorage', () => {
    const store = useAuthStore()
    store.setAuth('new.jwt.token', 'TEACHER', 'teacher1', 'T001')

    expect(store.token).toBe('new.jwt.token')
    expect(store.role).toBe('TEACHER')
    expect(store.username).toBe('teacher1')
    expect(store.code).toBe('T001')
    expect(localStorage.getItem('token')).toBe('new.jwt.token')
    expect(localStorage.getItem('role')).toBe('TEACHER')
    expect(localStorage.getItem('username')).toBe('teacher1')
  })

  it('setAuth should handle missing code', () => {
    const store = useAuthStore()
    store.setAuth('jwt.token', 'STUDENT', 'student1')

    expect(store.code).toBe('')
    expect(localStorage.getItem('code')).toBe('')
  })

  it('logout should clear all state and localStorage', () => {
    localStorage.setItem('token', 'jwt.token')
    localStorage.setItem('role', 'STUDENT')
    localStorage.setItem('username', 'testuser')

    setActivePinia(createPinia())
    const store = useAuthStore()
    store.logout()

    expect(store.token).toBe('')
    expect(store.role).toBe('')
    expect(store.username).toBe('')
    expect(store.code).toBe('')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('isLoggedIn should return true when token exists', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn()).toBe(false)

    store.setAuth('jwt.token', 'STUDENT', 'testuser')
    expect(store.isLoggedIn()).toBe(true)
  })

  it('role checkers should return correct boolean', () => {
    const store = useAuthStore()

    store.setAuth('token', 'STUDENT', 'user')
    expect(store.isStudent()).toBe(true)
    expect(store.isTeacher()).toBe(false)
    expect(store.isAdmin()).toBe(false)

    store.setAuth('token', 'TEACHER', 'user')
    expect(store.isStudent()).toBe(false)
    expect(store.isTeacher()).toBe(true)
    expect(store.isAdmin()).toBe(false)

    store.setAuth('token', 'ADMIN', 'user')
    expect(store.isStudent()).toBe(false)
    expect(store.isTeacher()).toBe(false)
    expect(store.isAdmin()).toBe(true)
  })
})
