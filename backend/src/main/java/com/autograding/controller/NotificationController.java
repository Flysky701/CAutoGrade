package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.Notification;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    public NotificationController(NotificationService notificationService, UserMapper userMapper) {
        this.notificationService = notificationService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<List<Notification>> getMyNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getNotificationsByUser(userId));
    }

    @GetMapping("/unread")
    public Result<List<Notification>> getUnreadNotifications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PostMapping
    public Result<Notification> createNotification(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String typeStr = (String) body.get("type");
        Notification.Type type = Notification.Type.valueOf(typeStr);
        Long relatedId = body.get("relatedId") != null
                ? Long.valueOf(body.get("relatedId").toString()) : null;
        Notification notification = notificationService.createNotification(
                userId, title, content, type, relatedId);
        return Result.success(notification);
    }

    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success(null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success(null);
    }

}
