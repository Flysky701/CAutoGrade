package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Announcement;
import com.autograding.entity.Course;
import com.autograding.mapper.AnnouncementMapper;
import com.autograding.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementMapper announcementMapper;
    @Mock private CourseMapper courseMapper;

    @InjectMocks
    private AnnouncementService announcementService;

    private Course course;
    private Announcement announcement;
    private Long teacherId = 1L;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        course.setName("C语言程序设计");
        course.setTeacherId(teacherId);
        course.setDeleted(0);

        announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("课程通知");
        announcement.setContent("周一上课提醒");
        announcement.setCourseId(10L);
        announcement.setPublisherId(teacherId);
        announcement.setIsPinned(0);
        announcement.setDeleted(0);
    }

    @Test
    void createAnnouncement_shouldSucceed() {
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(announcementMapper.insert(any(Announcement.class))).thenReturn(1);

        Announcement newAnn = new Announcement();
        newAnn.setTitle("课程通知");
        newAnn.setCourseId(10L);

        Announcement result = announcementService.createAnnouncement(newAnn, teacherId);

        assertNotNull(result);
        assertEquals(teacherId, result.getPublisherId());
        verify(announcementMapper).insert(newAnn);
    }

    @Test
    void createAnnouncement_shouldThrowWhenCourseNotFound() {
        when(courseMapper.selectById(10L)).thenReturn(null);

        Announcement newAnn = new Announcement();
        newAnn.setTitle("课程通知");
        newAnn.setCourseId(10L);

        assertThrows(BusinessException.class,
                () -> announcementService.createAnnouncement(newAnn, teacherId));
    }

    @Test
    void createAnnouncement_shouldThrowWhenNotOwner() {
        course.setTeacherId(99L);
        when(courseMapper.selectById(10L)).thenReturn(course);

        Announcement newAnn = new Announcement();
        newAnn.setTitle("课程通知");
        newAnn.setCourseId(10L);

        assertThrows(BusinessException.class,
                () -> announcementService.createAnnouncement(newAnn, teacherId));
    }

    @Test
    void getAnnouncementsByCourse_shouldReturnList() {
        when(announcementMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(announcement));

        List<Announcement> results = announcementService.getAnnouncementsByCourse(10L);

        assertEquals(1, results.size());
        assertEquals("课程通知", results.get(0).getTitle());
    }

    @Test
    void getAnnouncementById_shouldSucceed() {
        when(announcementMapper.selectById(1L)).thenReturn(announcement);

        Announcement result = announcementService.getAnnouncementById(1L);

        assertEquals("课程通知", result.getTitle());
    }

    @Test
    void getAnnouncementById_shouldThrowWhenNotFound() {
        when(announcementMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> announcementService.getAnnouncementById(99L));
    }

    @Test
    void updateAnnouncement_shouldThrowWhenNotPublisher() {
        when(announcementMapper.selectById(1L)).thenReturn(announcement);

        Announcement update = new Announcement();
        update.setTitle("更新通知");

        assertThrows(BusinessException.class,
                () -> announcementService.updateAnnouncement(1L, update, 99L));
    }
}
