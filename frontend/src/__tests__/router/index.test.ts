import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'

// Define routes matching the app structure
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: { template: '<div>Login</div>' },
  },
  {
    path: '/register',
    name: 'Register',
    component: { template: '<div>Register</div>' },
  },
  {
    path: '/student',
    name: 'StudentLayout',
    component: { template: '<div>Student Layout<router-view/></div>' },
    meta: { role: 'STUDENT' },
    children: [
      { path: 'dashboard', name: 'StudentDashboard', component: { template: '<div>Dashboard</div>' } },
    ],
  },
  {
    path: '/teacher',
    name: 'TeacherLayout',
    component: { template: '<div>Teacher Layout<router-view/></div>' },
    meta: { role: 'TEACHER' },
    children: [
      { path: 'dashboard', name: 'TeacherDashboard', component: { template: '<div>Dashboard</div>' } },
    ],
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: { template: '<div>Admin Layout<router-view/></div>' },
    meta: { role: 'ADMIN' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: { template: '<div>Dashboard</div>' } },
    ],
  },
  {
    path: '/',
    redirect: '/login',
  },
]

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes,
  })
}

describe('Router Guards', () => {
  let router: ReturnType<typeof createTestRouter>

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    router = createTestRouter()

    // Add guard logic (same as real app)
    router.beforeEach((to, _from, next) => {
      if (to.path === '/login' || to.path === '/register') {
        next()
        return
      }

      const token = localStorage.getItem('token')
      if (!token) {
        next('/login')
        return
      }

      const role = localStorage.getItem('role')
      const requiredRole = to.meta?.role as string | undefined
      if (requiredRole && role !== requiredRole) {
        next('/login')
        return
      }

      next()
    })
  })

  it('should allow navigation to login page without token', async () => {
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should allow navigation to register page without token', async () => {
    await router.push('/register')
    expect(router.currentRoute.value.path).toBe('/register')
  })

  it('should redirect to login when no token', async () => {
    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should allow student to access student routes', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    localStorage.setItem('role', 'STUDENT')

    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/student/dashboard')
  })

  it('should redirect student trying to access teacher routes', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    localStorage.setItem('role', 'STUDENT')

    await router.push('/teacher/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should allow teacher to access teacher routes', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    localStorage.setItem('role', 'TEACHER')

    await router.push('/teacher/dashboard')
    expect(router.currentRoute.value.path).toBe('/teacher/dashboard')
  })

  it('should redirect teacher trying to access admin routes', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    localStorage.setItem('role', 'TEACHER')

    await router.push('/admin/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should allow admin to access admin routes', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    localStorage.setItem('role', 'ADMIN')

    await router.push('/admin/dashboard')
    expect(router.currentRoute.value.path).toBe('/admin/dashboard')
  })

  it('should redirect root to login', async () => {
    await router.push('/')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('should redirect to login when role is empty', async () => {
    localStorage.setItem('token', 'valid.jwt.token.here')
    // No role set

    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
