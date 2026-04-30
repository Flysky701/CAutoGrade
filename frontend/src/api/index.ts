import api from './request';

// 后端统一返回 Result<T> = { code: number; msg: string; data: T }
interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

interface AuthPayload {
  token: string
  username: string
  role: string
  code: string
}

export const authApi = {
  login: (data: { username: string; password: string }) =>
    api.post<ApiResult<AuthPayload>>('/auth/login', data),
  register: (data: { username: string; password: string; nickname: string; role: string; code?: string }) =>
    api.post<ApiResult<AuthPayload>>('/auth/register', data),
};

export const userApi = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data: { nickname?: string; avatar?: string; code?: string }) =>
    api.put('/users/profile', data),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    api.post('/users/password', data),
  getAllUsers: () => api.get('/users'),
  getUsersByRole: (role: string) => api.get(`/users/role/${role}`),
  disableUser: (id: number) => api.post(`/users/${id}/disable`),
  enableUser: (id: number) => api.post(`/users/${id}/enable`),
  resetPassword: (id: number, newPassword: string) =>
    api.post(`/users/${id}/reset-password?newPassword=${newPassword}`),
};

export const adminApi = {
  getUsers: (role?: string) => api.get('/admin/users', { params: role ? { role } : undefined }),
  createUser: (data: any) => api.post('/admin/users', data),
  updateUser: (id: number, data: any) => api.put(`/admin/users/${id}`, data),
  deleteUser: (id: number) => api.delete(`/admin/users/${id}`),
  getLogs: (limit?: number) => api.get('/admin/logs', { params: limit ? { limit } : undefined }),
  getLogsByUser: (userId: number, limit?: number) => api.get(`/admin/logs/user/${userId}`, { params: limit ? { limit } : undefined }),
};

export const courseApi = {
  getAll: () => api.get('/courses'),
  getById: (id: number) => api.get(`/courses/${id}`),
  getMyCourses: () => api.get('/courses/teacher'),
  create: (data: any) => api.post('/courses', data),
  update: (id: number, data: any) => api.put(`/courses/${id}`, data),
  delete: (id: number) => api.delete(`/courses/${id}`),
};

export const classApi = {
  getByCourse: (courseId: number) => api.get(`/classes/course/${courseId}`),
  getMyClasses: () => api.get('/classes/teacher'),
  getMyClassesAsStudent: () => api.get('/classes/student'),
  create: (data: any) => api.post('/classes', data),
  join: (inviteCode: string) => api.post('/classes/join', null, { params: { inviteCode } }),
  getClassStudents: (classId: number) => api.get(`/classes/${classId}/students`),
  addStudent: (classId: number, studentId: number) =>
    api.post(`/classes/${classId}/students/${studentId}`),
  removeStudent: (classId: number, studentId: number) =>
    api.delete(`/classes/${classId}/students/${studentId}`),
  delete: (classId: number) => api.delete(`/classes/${classId}`),
};

export const assignmentApi = {
  getByCourse: (courseId: number) => api.get(`/assignments/course/${courseId}`),
  getMyAssignments: () => api.get('/assignments/teacher'),
  getStudentAssignments: () => api.get('/assignments/student'),
  getById: (id: number) => api.get(`/assignments/${id}`),
  getProblems: (id: number) => api.get(`/assignments/${id}/problems`),
  getProblemDetails: (id: number) => api.get(`/assignments/${id}/problem-details`),
  create: (data: any) => api.post('/assignments', data),
  update: (id: number, data: any) => api.put(`/assignments/${id}`, data),
  publish: (id: number) => api.post(`/assignments/${id}/publish`),
  delete: (id: number) => api.delete(`/assignments/${id}`),
};

export const problemApi = {
  getMyProblems: () => api.get('/problems'),
  getPublic: () => api.get('/problems/public'),
  getById: (id: number) => api.get(`/problems/${id}`),
  create: (data: any) => api.post('/problems', data),
  update: (id: number, data: any) => api.put(`/problems/${id}`, data),
  delete: (id: number) => api.delete(`/problems/${id}`),
};

export const testCaseApi = {
  getByProblem: (problemId: number) => api.get(`/test-cases/problem/${problemId}`),
  getVisible: (problemId: number) => api.get(`/test-cases/problem/${problemId}/visible`),
  create: (data: any) => api.post('/test-cases', data),
  update: (id: number, data: any) => api.put(`/test-cases/${id}`, data),
  delete: (id: number) => api.delete(`/test-cases/${id}`),
};

export const submissionApi = {
  submit: (data: { assignmentId: number; problemId: number; code: string }) =>
    api.post('/submissions', data),
  getById: (id: number) => api.get(`/submissions/${id}`),
  getGradingResult: (id: number) => api.get(`/submissions/${id}/grading`),
  getMySubmissions: () => api.get('/submissions/student'),
  getByAssignment: (assignmentId: number) => api.get(`/submissions/assignment/${assignmentId}`),
  getScoresByAssignment: (assignmentId: number) => api.get(`/submissions/scores/assignment/${assignmentId}`),
};

export const notificationApi = {
  getMy: () => api.get('/notifications'),
  getUnread: () => api.get('/notifications/unread'),
  getUnreadCount: () => api.get('/notifications/unread/count'),
  markAsRead: (id: number) => api.post(`/notifications/${id}/read`),
  markAllAsRead: () => api.post('/notifications/read-all'),
};

export const announcementApi = {
  getByCourse: (courseId: number) => api.get(`/announcements/course/${courseId}`),
  getById: (id: number) => api.get(`/announcements/${id}`),
  create: (data: any) => api.post('/announcements', data),
  update: (id: number, data: any) => api.put(`/announcements/${id}`, data),
  delete: (id: number) => api.delete(`/announcements/${id}`),
};

export const analyticsApi = {
  getClassAnalytics: (classId: number) => api.get(`/analytics/class/${classId}`),
  getStudentAnalytics: (studentId: number) => api.get(`/analytics/student/${studentId}`),
  getAssignmentAnalytics: (assignmentId: number) => api.get(`/analytics/assignment/${assignmentId}`),
  getProblemAnalytics: (problemId: number) => api.get(`/analytics/problem/${problemId}`),
};

export const gradingApi = {
  getPending: () => api.get('/gradings/pending'),
  getByAssignment: (assignmentId: number) => api.get(`/gradings/assignment/${assignmentId}`),
  getUnreviewed: () => api.get('/gradings/unreviewed'),
  review: (id: number, adjustedScore: number, feedback?: string) =>
    api.put(`/submissions/grading/${id}/review`, null, { params: { adjustedScore, feedback: feedback || '' } }),
};
