import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    delete (window as any).location
    ;(window as any).location = { href: '' }
  })

  it('can should return true for matching role', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'STUDENT', 'user')
    const { can } = usePermission()

    expect(can('student')).toBe(true)
    expect(can('teacher')).toBe(false)
    expect(can('admin')).toBe(false)
  })

  it('can should return true for teacher role', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'TEACHER', 'teacher1')
    const { can } = usePermission()

    expect(can('teacher')).toBe(true)
    expect(can('student')).toBe(false)
    expect(can('admin')).toBe(false)
  })

  it('can should return true for admin role', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'ADMIN', 'admin')
    const { can } = usePermission()

    expect(can('admin')).toBe(true)
    expect(can('teacher')).toBe(false)
    expect(can('student')).toBe(false)
  })

  it('can should return false for all when not logged in', () => {
    const { can } = usePermission()
    expect(can('student')).toBe(false)
    expect(can('teacher')).toBe(false)
    expect(can('admin')).toBe(false)
  })

  it('redirectByRole should navigate to admin dashboard for ADMIN', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'ADMIN', 'admin')
    const { redirectByRole } = usePermission()

    redirectByRole()
    expect(window.location.href).toBe('/admin/dashboard')
  })

  it('redirectByRole should navigate to teacher dashboard for TEACHER', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'TEACHER', 'teacher')
    const { redirectByRole } = usePermission()

    redirectByRole()
    expect(window.location.href).toBe('/teacher/dashboard')
  })

  it('redirectByRole should navigate to student dashboard for STUDENT', () => {
    const auth = useAuthStore()
    auth.setAuth('token', 'STUDENT', 'student')
    const { redirectByRole } = usePermission()

    redirectByRole()
    expect(window.location.href).toBe('/student/dashboard')
  })
})
