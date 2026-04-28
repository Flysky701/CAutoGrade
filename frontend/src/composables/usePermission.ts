import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  function can(required: 'student' | 'teacher' | 'admin') {
    const map = { student: 'STUDENT', teacher: 'TEACHER', admin: 'ADMIN' }
    return auth.role === map[required]
  }

  function redirectByRole() {
    if (auth.isAdmin()) window.location.href = '/admin/dashboard'
    else if (auth.isTeacher()) window.location.href = '/teacher/dashboard'
    else window.location.href = '/student/dashboard'
  }

  return { can, redirectByRole }
}
