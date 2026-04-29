import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
  },
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('../views/student/Layout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('../views/student/Dashboard.vue') },
      { path: 'courses', name: 'StudentCourses', component: () => import('../views/student/CourseList.vue') },
      { path: 'assignments/:courseId', name: 'StudentAssignments', component: () => import('../views/student/AssignmentList.vue') },
      { path: 'assignment/:assignmentId', name: 'AssignmentDetail', component: () => import('../views/student/AssignmentDetail.vue') },
      { path: 'submit/:assignmentId/:problemId', name: 'CodeSubmit', component: () => import('../views/student/CodeSubmit.vue') },
      { path: 'grading/:submissionId', name: 'GradingResult', component: () => import('../views/student/GradingResult.vue') },
      { path: 'history', name: 'SubmissionHistory', component: () => import('../views/student/SubmissionHistory.vue') },
      { path: 'profile', name: 'StudentProfile', component: () => import('../views/student/Profile.vue') },
    ],
  },
  {
    path: '/teacher',
    name: 'TeacherLayout',
    component: () => import('../views/teacher/Layout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', name: 'TeacherDashboard', component: () => import('../views/teacher/Dashboard.vue') },
      { path: 'courses', name: 'TeacherCourses', component: () => import('../views/teacher/CourseManage.vue') },
      { path: 'classes/:courseId', name: 'TeacherClasses', component: () => import('../views/teacher/ClassManage.vue') },
      { path: 'assignments', name: 'TeacherAssignments', component: () => import('../views/teacher/AssignmentPublish.vue') },
      { path: 'problems', name: 'ProblemBank', component: () => import('../views/teacher/ProblemBank.vue') },
      { path: 'grading-review', name: 'GradingReview', component: () => import('../views/teacher/GradingReview.vue') },
      { path: 'analytics', name: 'TeacherAnalytics', component: () => import('../views/teacher/Analytics.vue') },
      { path: 'announcements', name: 'TeacherAnnouncements', component: () => import('../views/teacher/Announcement.vue') },
      { path: 'score-export', name: 'ScoreExport', component: () => import('../views/teacher/ScoreExport.vue') },
      { path: 'profile', name: 'TeacherProfile', component: () => import('../views/teacher/Profile.vue') },
    ],
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('../views/admin/Layout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue') },
      { path: 'courses', name: 'AdminCourses', component: () => import('../views/admin/AdminCourses.vue') },
      { path: 'classes', name: 'AdminClasses', component: () => import('../views/admin/AdminClasses.vue') },
      { path: 'config', name: 'SystemConfig', component: () => import('../views/admin/SystemConfig.vue') },
      { path: 'logs', name: 'OperationLog', component: () => import('../views/admin/OperationLog.vue') },
    ],
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/common/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

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

export default router
