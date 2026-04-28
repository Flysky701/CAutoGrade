package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.CourseCreateRequest;
import com.autograding.dto.course.CourseResponse;
import com.autograding.entity.Course;
import com.autograding.entity.User;
import com.autograding.mapper.CourseMapper;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseMapper courseMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private CourseService courseService;

    private User teacher;
    private Course course;
    private CourseCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setNickname("张老师");
        teacher.setRole(User.Role.TEACHER);

        course = new Course();
        course.setId(10L);
        course.setName("C语言程序设计");
        course.setDescription("2026春季学期");
        course.setTeacherId(1L);
        course.setSemester("2026-SPRING");
        course.setStatus(Course.Status.ACTIVE);
        course.setDeleted(0);

        createRequest = new CourseCreateRequest();
        createRequest.setName("C语言程序设计");
        createRequest.setDescription("2026春季学期");
        createRequest.setSemester("2026-SPRING");
    }

    @Test
    void createCourse_shouldSucceed() {
        when(userMapper.selectById(1L)).thenReturn(teacher);
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        CourseResponse response = courseService.createCourse(createRequest, 1L);

        assertNotNull(response);
        assertEquals("C语言程序设计", response.getName());
        assertEquals("2026-SPRING", response.getSemester());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTeacherId());
        assertEquals(Course.Status.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void createCourse_shouldThrowWhenTeacherNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> courseService.createCourse(createRequest, 99L));
    }

    @Test
    void createCourse_shouldThrowWhenNotTeacher() {
        teacher.setRole(User.Role.STUDENT);
        when(userMapper.selectById(1L)).thenReturn(teacher);

        assertThrows(BusinessException.class,
                () -> courseService.createCourse(createRequest, 1L));
    }

    @Test
    void getCoursesByTeacher_shouldReturnList() {
        course.setTeacher(teacher);
        when(courseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(course));

        List<CourseResponse> results = courseService.getCoursesByTeacher(1L);

        assertEquals(1, results.size());
        assertEquals("张老师", results.get(0).getTeacherName());
    }

    @Test
    void getCourseById_shouldReturnCourse() {
        course.setTeacher(teacher);
        when(courseMapper.selectById(10L)).thenReturn(course);

        CourseResponse response = courseService.getCourseById(10L);

        assertEquals("C语言程序设计", response.getName());
        assertEquals("张老师", response.getTeacherName());
    }

    @Test
    void getCourseById_shouldThrowWhenNotFound() {
        when(courseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> courseService.getCourseById(99L));
    }

    @Test
    void getCourseById_shouldThrowWhenDeleted() {
        course.setDeleted(1);
        when(courseMapper.selectById(10L)).thenReturn(course);

        assertThrows(BusinessException.class,
                () -> courseService.getCourseById(10L));
    }

    @Test
    void updateCourse_shouldThrowWhenNotOwner() {
        when(courseMapper.selectById(10L)).thenReturn(course);

        CourseCreateRequest updateReq = new CourseCreateRequest();
        updateReq.setName("C语言进阶");

        assertThrows(BusinessException.class,
                () -> courseService.updateCourse(10L, updateReq, 99L));
    }

    @Test
    void deleteCourse_shouldThrowWhenNotOwner() {
        when(courseMapper.selectById(10L)).thenReturn(course);

        assertThrows(BusinessException.class,
                () -> courseService.deleteCourse(10L, 99L));
    }
}
