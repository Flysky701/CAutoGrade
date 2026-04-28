import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')

  function setAuth(t: string, r: string, u: string) {
    token.value = t
    role.value = r
    username.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('role', r)
    localStorage.setItem('username', u)
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
    window.location.href = '/login'
  }

  const isLoggedIn = () => !!token.value
  const isStudent = () => role.value === 'STUDENT'
  const isTeacher = () => role.value === 'TEACHER'
  const isAdmin = () => role.value === 'ADMIN'

  return { token, role, username, setAuth, logout, isLoggedIn, isStudent, isTeacher, isAdmin }
})
