package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Assignment;
import com.autograding.entity.AssignmentProblem;
import com.autograding.entity.Class;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.AssignmentProblemMapper;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentMapper assignmentMapper;
    @Mock private AssignmentProblemMapper assignmentProblemMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private ClassMapper classMapper;
    @Mock private ClassStudentMapper classStudentMapper;

    @InjectMocks
    private AssignmentService assignmentService;

    private Course course;
    private Assignment assignment;
    private Long teacherId = 1L;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        course.setName("C语言程序设计");
        course.setTeacherId(teacherId);
        course.setDeleted(0);

        assignment = new Assignment();
        assignment.setId(100L);
        assignment.setTitle("第一次作业");
        assignment.setCourseId(10L);
        assignment.setCreatedBy(teacherId);
        assignment.setStatus(Assignment.Status.DRAFT);
        assignment.setStartTime(LocalDateTime.now().minusDays(1));
        assignment.setEndTime(LocalDateTime.now().plusDays(7));
        assignment.setMaxScore(100);
        assignment.setDeleted(0);
    }

    @Test
    void createAssignment_shouldSucceed() {
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(assignmentMapper.insert(any(Assignment.class))).thenReturn(1);
        when(assignmentProblemMapper.insert(any(AssignmentProblem.class))).thenReturn(1);

        Assignment result = assignmentService.createAssignment(assignment, teacherId, List.of(1L, 2L));

        assertNotNull(result);
        assertEquals(Assignment.Status.DRAFT, result.getStatus());
        assertEquals(teacherId, result.getCreatedBy());
        verify(assignmentProblemMapper, times(2)).insert(any(AssignmentProblem.class));
    }

    @Test
    void createAssignment_withoutProblems_shouldSucceed() {
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(assignmentMapper.insert(any(Assignment.class))).thenReturn(1);

        Assignment result = assignmentService.createAssignment(assignment, teacherId, null);

        assertNotNull(result);
        verify(assignmentProblemMapper, never()).insert(any());
    }

    @Test
    void createAssignment_withEmptyProblems_shouldSucceed() {
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(assignmentMapper.insert(any(Assignment.class))).thenReturn(1);

        Assignment result = assignmentService.createAssignment(assignment, teacherId, List.of());

        assertNotNull(result);
        verify(assignmentProblemMapper, never()).insert(any());
    }

    @Test
    void createAssignment_shouldThrowWhenCourseNotFound() {
        when(courseMapper.selectById(10L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> assignmentService.createAssignment(assignment, teacherId, null));
    }

    @Test
    void createAssignment_shouldThrowWhenNotOwner() {
        course.setTeacherId(99L);
        when(courseMapper.selectById(10L)).thenReturn(course);

        assertThrows(BusinessException.class,
                () -> assignmentService.createAssignment(assignment, teacherId, null));
    }

    @Test
    void getAssignmentsByCourse_shouldReturnList() {
        when(assignmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(assignment));

        List<Assignment> results = assignmentService.getAssignmentsByCourse(10L);

        assertEquals(1, results.size());
        assertEquals("第一次作业", results.get(0).getTitle());
    }

    @Test
    void getAssignmentById_shouldSucceed() {
        when(assignmentMapper.selectById(100L)).thenReturn(assignment);

        Assignment result = assignmentService.getAssignmentById(100L);

        assertEquals("第一次作业", result.getTitle());
    }

    @Test
    void getAssignmentById_shouldThrowWhenNotFound() {
        when(assignmentMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> assignmentService.getAssignmentById(99L));
    }

    @Test
    void getAssignmentProblems_shouldReturnList() {
        AssignmentProblem ap = new AssignmentProblem();
        ap.setAssignmentId(100L);
        ap.setProblemId(1L);
        ap.setSortOrder(0);
        when(assignmentProblemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(ap));

        List<AssignmentProblem> results = assignmentService.getAssignmentProblems(100L);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getProblemId());
    }

    @Test
    void publishAssignment_shouldThrowWhenNotOwner() {
        when(assignmentMapper.selectById(100L)).thenReturn(assignment);

        assertThrows(BusinessException.class,
                () -> assignmentService.publishAssignment(100L, 99L));
    }

    @Test
    void deleteAssignment_shouldThrowWhenNotOwner() {
        when(assignmentMapper.selectById(100L)).thenReturn(assignment);

        assertThrows(BusinessException.class,
                () -> assignmentService.deleteAssignment(100L, 99L));
    }
}
