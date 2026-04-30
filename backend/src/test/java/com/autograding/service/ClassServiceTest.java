package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.ClassCreateRequest;
import com.autograding.dto.course.ClassResponse;
import com.autograding.entity.Class;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.entity.User;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.CourseMapper;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock private ClassMapper classMapper;
    @Mock private ClassStudentMapper classStudentMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private UserMapper userMapper;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private ClassService classService;

    private Course course;
    private Class cls;
    private User student;
    private User teacher;
    private Long teacherId = 1L;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setRole(User.Role.TEACHER);
        teacher.setDeleted(0);

        course = new Course();
        course.setId(10L);
        course.setName("C语言程序设计");
        course.setTeacherId(teacherId);
        course.setDeleted(0);

        cls = new Class();
        cls.setId(100L);
        cls.setName("一班");
        cls.setCourseId(10L);
        cls.setInviteCode("ABC12345");
        cls.setDeleted(0);

        student = new User();
        student.setId(5L);
        student.setUsername("student1");
        student.setNickname("小明");
        student.setRole(User.Role.STUDENT);
        student.setDeleted(0);

        var auth = new UsernamePasswordAuthenticationToken(
            new org.springframework.security.core.userdetails.User("teacher1", "", List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))),
            null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createClass_shouldSucceed() {
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(teacher);
        when(classMapper.insert(any(Class.class))).thenReturn(1);

        ClassCreateRequest request = new ClassCreateRequest();
        request.setName("一班");
        request.setCourseId(10L);

        ClassResponse response = classService.createClass(request, teacherId);

        assertNotNull(response);
        assertEquals("一班", response.getName());
        assertEquals(0, response.getStudentCount());
        verify(classMapper).insert(any(Class.class));
    }

    @Test
    void createClass_shouldThrowWhenCourseNotFound() {
        when(courseMapper.selectById(10L)).thenReturn(null);

        ClassCreateRequest request = new ClassCreateRequest();
        request.setName("一班");
        request.setCourseId(10L);

        assertThrows(BusinessException.class,
                () -> classService.createClass(request, teacherId));
    }

    @Test
    void createClass_shouldThrowWhenNotOwner() {
        course.setTeacherId(99L);
        when(courseMapper.selectById(10L)).thenReturn(course);

        ClassCreateRequest request = new ClassCreateRequest();
        request.setName("一班");
        request.setCourseId(10L);

        assertThrows(BusinessException.class,
                () -> classService.createClass(request, teacherId));
    }

    @Test
    void getClassesByCourse_shouldReturnList() {
        when(classMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(cls));
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        List<ClassResponse> results = classService.getClassesByCourse(10L);

        assertEquals(1, results.size());
        assertEquals("一班", results.get(0).getName());
        assertEquals(5, results.get(0).getStudentCount());
    }

    @Test
    void getClassById_shouldSucceed() {
        when(classMapper.selectById(100L)).thenReturn(cls);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        ClassResponse response = classService.getClassById(100L);

        assertEquals("一班", response.getName());
        assertEquals(3, response.getStudentCount());
    }

    @Test
    void getClassById_shouldThrowWhenNotFound() {
        when(classMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> classService.getClassById(99L));
    }

    @Test
    void joinClassByCode_shouldSucceed() {
        when(classMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cls);
        when(classStudentMapper.insert(any(ClassStudent.class))).thenReturn(1);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L, 1L);

        ClassResponse response = classService.joinClassByCode("ABC12345", 5L);

        assertNotNull(response);
        verify(classStudentMapper).insert(any(ClassStudent.class));
    }

    @Test
    void joinClassByCode_shouldThrowWhenInvalidCode() {
        when(classMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> classService.joinClassByCode("INVALID", 5L));
    }

    @Test
    void joinClassByCode_shouldThrowWhenAlreadyJoined() {
        when(classMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cls);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> classService.joinClassByCode("ABC12345", 5L));
    }

    @Test
    void addStudentToClass_shouldSucceed() {
        when(classMapper.selectById(100L)).thenReturn(cls);
        when(userMapper.selectById(5L)).thenReturn(student);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(classStudentMapper.insert(any(ClassStudent.class))).thenReturn(1);

        assertDoesNotThrow(() -> classService.addStudentToClass(100L, 5L));
        verify(classStudentMapper).insert(any(ClassStudent.class));
    }

    @Test
    void addStudentToClass_shouldThrowWhenAlreadyInClass() {
        when(classMapper.selectById(100L)).thenReturn(cls);
        when(userMapper.selectById(5L)).thenReturn(student);
        when(classStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> classService.addStudentToClass(100L, 5L));
    }

    @Test
    void removeStudentFromClass_shouldSucceed() {
        when(classStudentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> classService.removeStudentFromClass(100L, 5L));
        verify(classStudentMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void getClassStudents_shouldReturnList() {
        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(new ClassStudent()));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(student));

        List<User> results = classService.getClassStudents(100L);

        assertEquals(1, results.size());
        assertEquals("小明", results.get(0).getNickname());
    }

    @Test
    void getClassStudents_shouldReturnEmptyWhenNoStudents() {
        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<User> results = classService.getClassStudents(100L);

        assertTrue(results.isEmpty());
    }

    @Test
    void deleteClass_shouldThrowWhenNotOwner() {
        when(classMapper.selectById(100L)).thenReturn(cls);
        when(courseMapper.selectById(10L)).thenReturn(course);

        assertThrows(BusinessException.class,
                () -> classService.deleteClass(100L, 99L));
    }
}
