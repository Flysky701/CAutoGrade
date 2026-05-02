export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  status: number
}

export interface Course {
  id: number
  name: string
  description: string
  teacherId: number
  teacherName?: string
  semester: string
  status: 'ACTIVE' | 'ARCHIVED'
}

export interface ClassInfo {
  id: number
  name: string
  courseId: number
  inviteCode: string
  studentCount?: number
}

export interface Assignment {
  id: number
  title: string
  description: string
  courseId: number
  startTime: string
  endTime: string
  maxScore: number
  type: 'EXAM' | 'LAB' | 'PRACTICE'
  status: 'DRAFT' | 'PUBLISHED' | 'EXPIRED' | 'ARCHIVED'
}

export interface Problem {
  id: number
  title: string
  description: string
  difficulty: number
  knowledgeTags: string[]
  isPublic: boolean
}

export interface TestCase {
  id: number
  problemId: number
  inputData: string
  expectedOutput: string
  isHidden: boolean
  weight: number
}

export interface Submission {
  id: number
  assignmentId: number
  problemId: number
  studentId: number
  codeContent: string
  submitCount: number
  isLate: boolean
}

export interface GradingResult {
  id: number
  submissionId: number
  totalScore: number
  correctnessScore: number
  styleScore: number
  efficiencyScore: number
  feedbackJson: any
  gradingStatus: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED'
}

export interface Notification {
  id: number
  title: string
  content: string
  type: 'ASSIGNMENT' | 'GRADING' | 'SYSTEM' | 'ANNOUNCEMENT'
  isRead: boolean
  createdAt: string
}
