import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')
  const code = ref(localStorage.getItem('code') || '')

  function setAuth(t: string, r: string, u: string, c?: string) {
    token.value = t
    role.value = r
    username.value = u
    code.value = c || ''
    localStorage.setItem('token', t)
    localStorage.setItem('role', r)
    localStorage.setItem('username', u)
    localStorage.setItem('code', c || '')
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    code.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
    localStorage.removeItem('code')
    window.location.href = '/login'
  }

  const isLoggedIn = () => !!token.value
  const isStudent = () => role.value === 'STUDENT'
  const isTeacher = () => role.value === 'TEACHER'
  const isAdmin = () => role.value === 'ADMIN'

  return { token, role, username, code, setAuth, logout, isLoggedIn, isStudent, isTeacher, isAdmin }
})
