package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.Announcement;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.AnnouncementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserMapper userMapper;

    public AnnouncementController(AnnouncementService announcementService, UserMapper userMapper) {
        this.announcementService = announcementService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public Result<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        Long publisherId = SecurityUtils.getCurrentUserId();
        return Result.success(announcementService.createAnnouncement(announcement, publisherId));
    }

    @GetMapping("/course/{courseId}")
    public Result<List<Announcement>> getAnnouncementsByCourse(@PathVariable Long courseId) {
        return Result.success(announcementService.getAnnouncementsByCourse(courseId));
    }

    @GetMapping("/student/my")
    public Result<List<Announcement>> getMyAnnouncements() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(announcementService.getAnnouncementsForStudent(studentId));
    }

    @GetMapping("/{id}")
    public Result<Announcement> getAnnouncementById(@PathVariable Long id) {
        return Result.success(announcementService.getAnnouncementById(id));
    }

    @PutMapping("/{id}")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id,
                                                   @RequestBody Announcement request) {
        Long publisherId = SecurityUtils.getCurrentUserId();
        return Result.success(announcementService.updateAnnouncement(id, request, publisherId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        Long publisherId = SecurityUtils.getCurrentUserId();
        announcementService.deleteAnnouncement(id, publisherId);
        return Result.success(null);
    }

}
