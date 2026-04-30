package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Announcement;
import com.autograding.entity.Class;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.mapper.AnnouncementMapper;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CourseMapper courseMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassMapper classMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper, CourseMapper courseMapper,
                               ClassStudentMapper classStudentMapper, ClassMapper classMapper) {
        this.announcementMapper = announcementMapper;
        this.courseMapper = courseMapper;
        this.classStudentMapper = classStudentMapper;
        this.classMapper = classMapper;
    }

    public Announcement createAnnouncement(Announcement announcement, Long publisherId) {
        Course course = courseMapper.selectById(announcement.getCourseId());
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(publisherId)) {
            throw new BusinessException("无权限在此课程发布公告");
        }

        announcement.setPublisherId(publisherId);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setDeleted(0);
        announcementMapper.insert(announcement);
        return announcement;
    }

    public List<Announcement> getAnnouncementsByCourse(Long courseId) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getCourseId, courseId)
               .eq(Announcement::getDeleted, 0)
               .orderByDesc(Announcement::getIsPinned)
               .orderByDesc(Announcement::getCreatedAt);
        return announcementMapper.selectList(wrapper);
    }

    public List<Announcement> getAnnouncementsForStudent(Long studentId) {
        // 1. 获取学生加入的所有班级
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        List<Long> classIds = classStudentMapper.selectList(csWrapper)
                .stream().map(ClassStudent::getClassId).collect(java.util.stream.Collectors.toList());
        if (classIds.isEmpty()) {
            return List.of();
        }

        // 2. 获取这些班级对应的课程ID
        List<Long> courseIds = classMapper.selectBatchIds(classIds)
                .stream().map(Class::getCourseId).distinct().collect(java.util.stream.Collectors.toList());
        if (courseIds.isEmpty()) {
            return List.of();
        }

        // 3. 查询这些课程的公告
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Announcement::getCourseId, courseIds)
               .eq(Announcement::getDeleted, 0)
               .orderByDesc(Announcement::getIsPinned)
               .orderByDesc(Announcement::getCreatedAt);
        return announcementMapper.selectList(wrapper);
    }

    public Announcement getAnnouncementById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            throw new BusinessException("公告不存在");
        }
        return announcement;
    }

    public Announcement updateAnnouncement(Long id, Announcement request, Long publisherId) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            throw new BusinessException("公告不存在");
        }
        if (!announcement.getPublisherId().equals(publisherId)) {
            throw new BusinessException("无权限修改此公告");
        }

        LambdaUpdateWrapper<Announcement> wrapper = new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .set(request.getTitle() != null, Announcement::getTitle, request.getTitle())
                .set(request.getContent() != null, Announcement::getContent, request.getContent())
                .set(request.getIsPinned() != null, Announcement::getIsPinned, request.getIsPinned());
        announcementMapper.update(null, wrapper);
        return getAnnouncementById(id);
    }

    public void deleteAnnouncement(Long id, Long publisherId) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            throw new BusinessException("公告不存在");
        }
        if (!announcement.getPublisherId().equals(publisherId)) {
            throw new BusinessException("无权限删除此公告");
        }

        LambdaUpdateWrapper<Announcement> wrapper = new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .set(Announcement::getDeleted, 1);
        announcementMapper.update(null, wrapper);
    }
}
