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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    public ClassService(ClassMapper classMapper,
                        ClassStudentMapper classStudentMapper,
                        CourseMapper courseMapper,
                        UserMapper userMapper) {
        this.classMapper = classMapper;
        this.classStudentMapper = classStudentMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
    }

    public ClassResponse createClass(ClassCreateRequest request, Long teacherId) {
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限在此课程下创建班级");
        }

        Class cls = new Class();
        cls.setName(request.getName());
        cls.setCourseId(request.getCourseId());
        cls.setInviteCode(generateInviteCode());
        cls.setCreatedAt(LocalDateTime.now());
        cls.setUpdatedAt(LocalDateTime.now());
        classMapper.insert(cls);

        cls.setCourse(course);
        ClassResponse response = ClassResponse.fromEntity(cls);
        response.setStudentCount(0);
        return response;
    }

    public List<ClassResponse> getClassesByCourse(Long courseId) {
        LambdaQueryWrapper<Class> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Class::getCourseId, courseId)
               .eq(Class::getDeleted, 0)
               .orderByDesc(Class::getCreatedAt);
        return classMapper.selectList(wrapper).stream()
                .map(this::enrichAndConvert)
                .collect(Collectors.toList());
    }

    public List<ClassResponse> getClassesByTeacher(Long teacherId) {
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getTeacherId, teacherId)
                     .eq(Course::getDeleted, 0);
        List<Long> courseIds = courseMapper.selectList(courseWrapper)
                .stream().map(Course::getId).collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.in(Class::getCourseId, courseIds)
                    .eq(Class::getDeleted, 0)
                    .orderByDesc(Class::getCreatedAt);
        return classMapper.selectList(classWrapper).stream()
                .map(this::enrichAndConvert)
                .collect(Collectors.toList());
    }

    public ClassResponse getClassById(Long id) {
        Class cls = classMapper.selectById(id);
        if (cls == null || cls.getDeleted() == 1) {
            throw new BusinessException("班级不存在");
        }
        return enrichAndConvert(cls);
    }

    public ClassResponse joinClassByCode(String inviteCode, Long studentId) {
        LambdaQueryWrapper<Class> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Class::getInviteCode, inviteCode)
               .eq(Class::getDeleted, 0);
        Class cls = classMapper.selectOne(wrapper);
        if (cls == null) {
            throw new BusinessException("无效的选课码");
        }

        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, cls.getId())
                 .eq(ClassStudent::getStudentId, studentId);
        if (classStudentMapper.selectCount(csWrapper) > 0) {
            throw new BusinessException("您已在此班级中");
        }

        ClassStudent cs = new ClassStudent();
        cs.setClassId(cls.getId());
        cs.setStudentId(studentId);
        cs.setJoinedAt(LocalDateTime.now());
        classStudentMapper.insert(cs);

        return enrichAndConvert(cls);
    }

    public List<ClassResponse> getClassesByStudent(Long studentId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        List<Long> classIds = classStudentMapper.selectList(csWrapper)
                .stream().map(ClassStudent::getClassId).collect(Collectors.toList());

        if (classIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Class> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Class::getId, classIds)
               .eq(Class::getDeleted, 0);
        return classMapper.selectList(wrapper).stream()
                .map(this::enrichAndConvert)
                .collect(Collectors.toList());
    }

    public void addStudentToClass(Long classId, Long studentId) {
        Class cls = classMapper.selectById(classId);
        if (cls == null || cls.getDeleted() == 1) {
            throw new BusinessException("班级不存在");
        }

        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId)
               .eq(ClassStudent::getStudentId, studentId);
        if (classStudentMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("学生已在班级中");
        }

        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        cs.setJoinedAt(LocalDateTime.now());
        classStudentMapper.insert(cs);
    }

    public void removeStudentFromClass(Long classId, Long studentId) {
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId)
               .eq(ClassStudent::getStudentId, studentId);
        classStudentMapper.delete(wrapper);
    }

    public List<User> getClassStudents(Long classId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        List<Long> studentIds = classStudentMapper.selectList(csWrapper)
                .stream().map(ClassStudent::getStudentId).collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            return List.of();
        }

        return userMapper.selectBatchIds(studentIds);
    }

    public void deleteClass(Long id, Long teacherId) {
        Class cls = classMapper.selectById(id);
        if (cls == null || cls.getDeleted() == 1) {
            throw new BusinessException("班级不存在");
        }

        Course course = courseMapper.selectById(cls.getCourseId());
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限删除此班级");
        }

        LambdaUpdateWrapper<Class> wrapper = new LambdaUpdateWrapper<Class>()
                .eq(Class::getId, id)
                .set(Class::getDeleted, 1)
                .set(Class::getUpdatedAt, LocalDateTime.now());
        classMapper.update(null, wrapper);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ClassResponse enrichAndConvert(Class cls) {
        if (cls.getCourse() == null && cls.getCourseId() != null) {
            Course course = courseMapper.selectById(cls.getCourseId());
            cls.setCourse(course);
        }

        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, cls.getId());
        int studentCount = classStudentMapper.selectCount(csWrapper).intValue();

        ClassResponse response = ClassResponse.fromEntity(cls);
        response.setStudentCount(studentCount);
        return response;
    }
}
