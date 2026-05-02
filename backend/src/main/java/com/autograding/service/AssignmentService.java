package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Assignment;
import com.autograding.entity.AssignmentProblem;
import com.autograding.entity.Class;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.entity.Problem;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.AssignmentProblemMapper;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.CourseMapper;
import com.autograding.mapper.ProblemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentProblemMapper assignmentProblemMapper;
    private final CourseMapper courseMapper;
    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ProblemMapper problemMapper;

    public AssignmentService(AssignmentMapper assignmentMapper,
                            AssignmentProblemMapper assignmentProblemMapper,
                            CourseMapper courseMapper,
                            ClassMapper classMapper,
                            ClassStudentMapper classStudentMapper,
                            ProblemMapper problemMapper) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentProblemMapper = assignmentProblemMapper;
        this.courseMapper = courseMapper;
        this.classMapper = classMapper;
        this.classStudentMapper = classStudentMapper;
        this.problemMapper = problemMapper;
    }

    @Transactional
    public Assignment createAssignment(Assignment assignment, Long teacherId, List<Long> problemIds) {
        Course course = courseMapper.selectById(assignment.getCourseId());
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限在此课程下创建作业");
        }

        assignment.setCreatedBy(teacherId);
        assignment.setStatus(Assignment.Status.DRAFT);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        if (problemIds != null && !problemIds.isEmpty()) {
            for (int i = 0; i < problemIds.size(); i++) {
                AssignmentProblem ap = new AssignmentProblem();
                ap.setAssignmentId(assignment.getId());
                ap.setProblemId(problemIds.get(i));
                ap.setSortOrder(i);
                assignmentProblemMapper.insert(ap);
            }
        }

        return assignment;
    }

    public List<Assignment> getAssignmentsByCourse(Long courseId) {
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assignment::getCourseId, courseId)
               .eq(Assignment::getDeleted, 0)
               .orderByDesc(Assignment::getEndTime);
        return assignmentMapper.selectList(wrapper);
    }

    public List<Assignment> getAssignmentsByTeacher(Long teacherId) {
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getTeacherId, teacherId)
                     .eq(Course::getDeleted, 0);
        List<Long> courseIds = courseMapper.selectList(courseWrapper)
                .stream().map(Course::getId).toList();

        if (courseIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Assignment::getCourseId, courseIds)
               .eq(Assignment::getDeleted, 0)
               .orderByDesc(Assignment::getEndTime);
        return assignmentMapper.selectList(wrapper);
    }

    public List<Assignment> getAssignmentsByStudent(Long studentId) {
        // 1. Find all classes the student belongs to
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        List<Long> classIds = classStudentMapper.selectList(csWrapper)
                .stream().map(ClassStudent::getClassId).toList();
        if (classIds.isEmpty()) {
            return List.of();
        }

        // 2. Find courses for those classes
        LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.in(Class::getId, classIds);
        List<Long> courseIds = classMapper.selectList(classWrapper)
                .stream().map(Class::getCourseId).distinct().toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }

        // 3. Find published assignments for those courses
        LambdaQueryWrapper<Assignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Assignment::getCourseId, courseIds)
               .eq(Assignment::getStatus, Assignment.Status.PUBLISHED)
               .eq(Assignment::getDeleted, 0)
               .orderByDesc(Assignment::getEndTime);
        return assignmentMapper.selectList(wrapper);
    }

    public Assignment getAssignmentById(Long id) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("作业不存在");
        }
        return assignment;
    }

    public List<AssignmentProblem> getAssignmentProblems(Long assignmentId) {
        LambdaQueryWrapper<AssignmentProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssignmentProblem::getAssignmentId, assignmentId)
               .orderByAsc(AssignmentProblem::getSortOrder);
        return assignmentProblemMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getProblemDetails(Long assignmentId) {
        List<AssignmentProblem> aps = getAssignmentProblems(assignmentId);
        if (aps.isEmpty()) {
            return List.of();
        }
        List<Long> problemIds = aps.stream().map(AssignmentProblem::getProblemId).toList();
        Map<Long, Problem> problemMap = problemMapper.selectBatchIds(problemIds)
                .stream().collect(Collectors.toMap(Problem::getId, p -> p, (a, b) -> a));

        return aps.stream().map(ap -> {
            Problem p = problemMap.get(ap.getProblemId());
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", ap.getId());
            item.put("assignmentId", ap.getAssignmentId());
            item.put("problemId", ap.getProblemId());
            item.put("sortOrder", ap.getSortOrder());
            if (p != null) {
                item.put("title", p.getTitle());
                item.put("description", p.getDescription());
                item.put("difficulty", p.getDifficulty());
                item.put("knowledgeTags", p.getKnowledgeTags());
                item.put("inputDesc", p.getInputDesc());
                item.put("outputDesc", p.getOutputDesc());
                item.put("timeLimitMs", p.getTimeLimitMs());
                item.put("memoryLimitKb", p.getMemoryLimitKb());
            }
            return item;
        }).toList();
    }

    @Transactional
    public Assignment updateAssignment(Long id, Assignment request, Long teacherId, List<Long> problemIds) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("作业不存在");
        }
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new BusinessException("无权限修改此作业");
        }

        LambdaUpdateWrapper<Assignment> wrapper = new LambdaUpdateWrapper<Assignment>()
                .eq(Assignment::getId, id)
                .set(request.getTitle() != null, Assignment::getTitle, request.getTitle())
                .set(request.getDescription() != null, Assignment::getDescription, request.getDescription())
                .set(request.getStartTime() != null, Assignment::getStartTime, request.getStartTime())
                .set(request.getEndTime() != null, Assignment::getEndTime, request.getEndTime())
                .set(request.getMaxScore() != null, Assignment::getMaxScore, request.getMaxScore())
                .set(request.getType() != null, Assignment::getType, request.getType())
                .set(request.getLatePenaltyScore() != null, Assignment::getLatePenaltyScore, request.getLatePenaltyScore())
                .set(Assignment::getUpdatedAt, LocalDateTime.now());
        assignmentMapper.update(null, wrapper);

        if (problemIds != null) {
            LambdaQueryWrapper<AssignmentProblem> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(AssignmentProblem::getAssignmentId, id);
            assignmentProblemMapper.delete(delWrapper);

            for (int i = 0; i < problemIds.size(); i++) {
                AssignmentProblem ap = new AssignmentProblem();
                ap.setAssignmentId(id);
                ap.setProblemId(problemIds.get(i));
                ap.setSortOrder(i);
                assignmentProblemMapper.insert(ap);
            }
        }

        return getAssignmentById(id);
    }

    public Assignment publishAssignment(Long id, Long teacherId) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("作业不存在");
        }
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new BusinessException("无权限发布此作业");
        }

        LambdaUpdateWrapper<Assignment> wrapper = new LambdaUpdateWrapper<Assignment>()
                .eq(Assignment::getId, id)
                .set(Assignment::getStatus, Assignment.Status.PUBLISHED)
                .set(Assignment::getUpdatedAt, LocalDateTime.now());
        assignmentMapper.update(null, wrapper);
        return getAssignmentById(id);
    }

    public void deleteAssignment(Long id, Long teacherId) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("作业不存在");
        }
        if (!assignment.getCreatedBy().equals(teacherId)) {
            throw new BusinessException("无权限删除此作业");
        }

        LambdaUpdateWrapper<Assignment> wrapper = new LambdaUpdateWrapper<Assignment>()
                .eq(Assignment::getId, id)
                .set(Assignment::getDeleted, 1)
                .set(Assignment::getUpdatedAt, LocalDateTime.now());
        assignmentMapper.update(null, wrapper);
    }
}
