package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Assignment;
import com.autograding.entity.AssignmentProblem;
import com.autograding.entity.Course;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.AssignmentProblemMapper;
import com.autograding.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentProblemMapper assignmentProblemMapper;
    private final CourseMapper courseMapper;

    public AssignmentService(AssignmentMapper assignmentMapper,
                            AssignmentProblemMapper assignmentProblemMapper,
                            CourseMapper courseMapper) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentProblemMapper = assignmentProblemMapper;
        this.courseMapper = courseMapper;
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
