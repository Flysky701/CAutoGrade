import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import api from '@/api/request'
import {
  authApi, userApi, adminApi, courseApi, classApi,
  assignmentApi, problemApi, testCaseApi, submissionApi,
  notificationApi, announcementApi, analyticsApi, gradingApi
} from '@/api/index'

const mockAxios = api as any

describe('API Layer Tests', () => {

  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ================== Auth API ==================
  describe('authApi', () => {
    it('login should POST to /auth/login with credentials', () => {
      authApi.login({ username: 'test', password: '123' })
      expect(mockAxios.post).toHaveBeenCalledWith('/auth/login', { username: 'test', password: '123' })
    })

    it('register should POST to /auth/register with user data', () => {
      authApi.register({ username: 'test', password: '123', nickname: 'Test', role: 'STUDENT' })
      expect(mockAxios.post).toHaveBeenCalledWith('/auth/register',
        { username: 'test', password: '123', nickname: 'Test', role: 'STUDENT' })
    })
  })

  // ================== User API ==================
  describe('userApi', () => {
    it('getProfile should GET /users/profile', () => {
      userApi.getProfile()
      expect(mockAxios.get).toHaveBeenCalledWith('/users/profile')
    })

    it('updateProfile should PUT /users/profile', () => {
      userApi.updateProfile({ nickname: 'NewName' })
      expect(mockAxios.put).toHaveBeenCalledWith('/users/profile', { nickname: 'NewName' })
    })

    it('changePassword should POST /users/password', () => {
      userApi.changePassword({ oldPassword: 'old', newPassword: 'new' })
      expect(mockAxios.post).toHaveBeenCalledWith('/users/password', { oldPassword: 'old', newPassword: 'new' })
    })

    it('getAllUsers should GET /users', () => {
      userApi.getAllUsers()
      expect(mockAxios.get).toHaveBeenCalledWith('/users')
    })

    it('getUsersByRole should GET /users/role/STUDENT', () => {
      userApi.getUsersByRole('STUDENT')
      expect(mockAxios.get).toHaveBeenCalledWith('/users/role/STUDENT')
    })

    it('disableUser should POST /users/1/disable', () => {
      userApi.disableUser(1)
      expect(mockAxios.post).toHaveBeenCalledWith('/users/1/disable')
    })
  })

  // ================== Admin API ==================
  describe('adminApi', () => {
    it('createUser should POST /admin/users', () => {
      adminApi.createUser({ username: 'newuser' })
      expect(mockAxios.post).toHaveBeenCalledWith('/admin/users', { username: 'newuser' })
    })

    it('deleteUser should DELETE /admin/users/5', () => {
      adminApi.deleteUser(5)
      expect(mockAxios.delete).toHaveBeenCalledWith('/admin/users/5')
    })

    it('getUsers should GET /admin/users', () => {
      adminApi.getUsers()
      expect(mockAxios.get).toHaveBeenCalledWith('/admin/users', { params: undefined })
    })
  })

  // ================== Course API ==================
  describe('courseApi', () => {
    it('getAll should GET /courses', () => {
      courseApi.getAll()
      expect(mockAxios.get).toHaveBeenCalledWith('/courses')
    })

    it('getById should GET /courses/10', () => {
      courseApi.getById(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/courses/10')
    })

    it('create should POST /courses', () => {
      courseApi.create({ name: 'C语言' })
      expect(mockAxios.post).toHaveBeenCalledWith('/courses', { name: 'C语言' })
    })

    it('update should PUT /courses/10', () => {
      courseApi.update(10, { name: 'C语言进阶' })
      expect(mockAxios.put).toHaveBeenCalledWith('/courses/10', { name: 'C语言进阶' })
    })

    it('delete should DELETE /courses/10', () => {
      courseApi.delete(10)
      expect(mockAxios.delete).toHaveBeenCalledWith('/courses/10')
    })
  })

  // ================== Class API ==================
  describe('classApi', () => {
    it('getByCourse should GET /classes/course/10', () => {
      classApi.getByCourse(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/classes/course/10')
    })

    it('create should POST /classes', () => {
      classApi.create({ name: '一班', courseId: 10 })
      expect(mockAxios.post).toHaveBeenCalledWith('/classes', { name: '一班', courseId: 10 })
    })

    it('join should POST /classes/join with inviteCode param', () => {
      classApi.join('ABC')
      expect(mockAxios.post).toHaveBeenCalledWith('/classes/join', null, { params: { inviteCode: 'ABC' } })
    })

    it('addStudent should POST /classes/100/students/5', () => {
      classApi.addStudent(100, 5)
      expect(mockAxios.post).toHaveBeenCalledWith('/classes/100/students/5')
    })

    it('removeStudent should DELETE /classes/100/students/5', () => {
      classApi.removeStudent(100, 5)
      expect(mockAxios.delete).toHaveBeenCalledWith('/classes/100/students/5')
    })
  })

  // ================== Assignment API ==================
  describe('assignmentApi', () => {
    it('getById should GET /assignments/1', () => {
      assignmentApi.getById(1)
      expect(mockAxios.get).toHaveBeenCalledWith('/assignments/1', undefined)
    })

    it('create should POST /assignments', () => {
      assignmentApi.create({ title: '作业一', courseId: 10 })
      expect(mockAxios.post).toHaveBeenCalledWith('/assignments', { title: '作业一', courseId: 10 })
    })

    it('publish should POST /assignments/1/publish', () => {
      assignmentApi.publish(1)
      expect(mockAxios.post).toHaveBeenCalledWith('/assignments/1/publish')
    })
  })

  // ================== Problem API ==================
  describe('problemApi', () => {
    it('getMyProblems should GET /problems', () => {
      problemApi.getMyProblems()
      expect(mockAxios.get).toHaveBeenCalledWith('/problems')
    })

    it('getPublic should GET /problems/public', () => {
      problemApi.getPublic()
      expect(mockAxios.get).toHaveBeenCalledWith('/problems/public')
    })

    it('create should POST /problems', () => {
      problemApi.create({ title: '两数之和', difficulty: 1 })
      expect(mockAxios.post).toHaveBeenCalledWith('/problems', { title: '两数之和', difficulty: 1 })
    })

    it('delete should DELETE /problems/1', () => {
      problemApi.delete(1)
      expect(mockAxios.delete).toHaveBeenCalledWith('/problems/1')
    })
  })

  // ================== TestCase API ==================
  describe('testCaseApi', () => {
    it('getByProblem should GET /test-cases/problem/10', () => {
      testCaseApi.getByProblem(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/test-cases/problem/10')
    })

    it('getVisible should GET /test-cases/problem/10/visible', () => {
      testCaseApi.getVisible(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/test-cases/problem/10/visible')
    })

    it('create should POST /test-cases', () => {
      testCaseApi.create({ input: '1 2', expectedOutput: '3' })
      expect(mockAxios.post).toHaveBeenCalledWith('/test-cases', { input: '1 2', expectedOutput: '3' })
    })
  })

  // ================== Submission API ==================
  describe('submissionApi', () => {
    it('submit should POST /submissions', () => {
      submissionApi.submit({ assignmentId: 1, problemId: 2, code: '#include <stdio.h>' })
      expect(mockAxios.post).toHaveBeenCalledWith('/submissions',
        { assignmentId: 1, problemId: 2, code: '#include <stdio.h>' })
    })

    it('getById should GET /submissions/1', () => {
      submissionApi.getById(1)
      expect(mockAxios.get).toHaveBeenCalledWith('/submissions/1')
    })

    it('getGradingResult should GET /submissions/1/grading', () => {
      submissionApi.getGradingResult(1)
      expect(mockAxios.get).toHaveBeenCalledWith('/submissions/1/grading')
    })
  })

  // ================== Notification API ==================
  describe('notificationApi', () => {
    it('getMy should GET /notifications', () => {
      notificationApi.getMy()
      expect(mockAxios.get).toHaveBeenCalledWith('/notifications')
    })

    it('getUnreadCount should GET /notifications/unread/count', () => {
      notificationApi.getUnreadCount()
      expect(mockAxios.get).toHaveBeenCalledWith('/notifications/unread/count')
    })

    it('markAsRead should POST /notifications/1/read', () => {
      notificationApi.markAsRead(1)
      expect(mockAxios.post).toHaveBeenCalledWith('/notifications/1/read')
    })

    it('markAllAsRead should POST /notifications/read-all', () => {
      notificationApi.markAllAsRead()
      expect(mockAxios.post).toHaveBeenCalledWith('/notifications/read-all')
    })
  })

  // ================== Announcement API ==================
  describe('announcementApi', () => {
    it('getByCourse should GET /announcements/course/10', () => {
      announcementApi.getByCourse(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/announcements/course/10')
    })

    it('create should POST /announcements', () => {
      announcementApi.create({ title: '通知', content: '内容' })
      expect(mockAxios.post).toHaveBeenCalledWith('/announcements', { title: '通知', content: '内容' })
    })

    it('delete should DELETE /announcements/1', () => {
      announcementApi.delete(1)
      expect(mockAxios.delete).toHaveBeenCalledWith('/announcements/1')
    })
  })

  // ================== Analytics API ==================
  describe('analyticsApi', () => {
    it('getClassAnalytics should GET /analytics/class/10', () => {
      analyticsApi.getClassAnalytics(10)
      expect(mockAxios.get).toHaveBeenCalledWith('/analytics/class/10')
    })

    it('getStudentAnalytics should GET /analytics/student/1', () => {
      analyticsApi.getStudentAnalytics(1)
      expect(mockAxios.get).toHaveBeenCalledWith('/analytics/student/1')
    })
  })

  // ================== Grading API ==================
  describe('gradingApi', () => {
    it('getPending should GET /gradings/pending', () => {
      gradingApi.getPending()
      expect(mockAxios.get).toHaveBeenCalledWith('/gradings/pending')
    })

    it('getByAssignment should GET /gradings/assignment/5', () => {
      gradingApi.getByAssignment(5)
      expect(mockAxios.get).toHaveBeenCalledWith('/gradings/assignment/5')
    })

    it('review should PUT /submissions/grading/1/review with params', () => {
      gradingApi.review(1, 90, 'Good')
      expect(mockAxios.put).toHaveBeenCalledWith('/submissions/grading/1/review', null, { params: { adjustedScore: 90, feedback: 'Good' } })
    })
  })
})
