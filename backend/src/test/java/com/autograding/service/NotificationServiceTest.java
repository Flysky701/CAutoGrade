package com.autograding.service;

import com.autograding.entity.Notification;
import com.autograding.mapper.NotificationMapper;
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
class NotificationServiceTest {

    @Mock private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setId(1L);
        notification.setUserId(10L);
        notification.setTitle("作业截止提醒");
        notification.setContent("作业将在24小时后截止");
        notification.setType(Notification.Type.ASSIGNMENT);
        notification.setRelatedId(5L);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createNotification_shouldSucceed() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        Notification result = notificationService.createNotification(
                10L, "新通知", "内容", Notification.Type.SYSTEM, null);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals("新通知", result.getTitle());
        assertEquals(Notification.Type.SYSTEM, result.getType());
        assertEquals(0, result.getIsRead());
        assertNotNull(result.getCreatedAt());
        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void getNotificationsByUser_shouldReturnList() {
        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(notification));

        List<Notification> results = notificationService.getNotificationsByUser(10L);

        assertEquals(1, results.size());
        assertEquals("作业截止提醒", results.get(0).getTitle());
    }

    @Test
    void getNotificationsByUser_shouldReturnEmptyList() {
        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<Notification> results = notificationService.getNotificationsByUser(10L);

        assertTrue(results.isEmpty());
    }

    @Test
    void getUnreadNotifications_shouldReturnFilteredList() {
        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(notification));

        List<Notification> results = notificationService.getUnreadNotifications(10L);

        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getIsRead());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        int count = notificationService.getUnreadCount(10L);

        assertEquals(3, count);
    }

    @Test
    void getUnreadCount_shouldReturnZero() {
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        int count = notificationService.getUnreadCount(10L);

        assertEquals(0, count);
    }

    @Test
    void markAsRead_shouldUpdateNotification() {
        when(notificationMapper.selectById(1L)).thenReturn(notification);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        notificationService.markAsRead(1L);

        assertEquals(1, notification.getIsRead());
        verify(notificationMapper).updateById(notification);
    }

    @Test
    void markAsRead_shouldNotThrowWhenNotFound() {
        when(notificationMapper.selectById(99L)).thenReturn(null);

        assertDoesNotThrow(() -> notificationService.markAsRead(99L));
        verify(notificationMapper, never()).updateById(any());
    }

    @Test
    void markAllAsRead_shouldUpdateAllUnread() {
        Notification unread2 = new Notification();
        unread2.setId(2L);
        unread2.setUserId(10L);
        unread2.setIsRead(0);

        when(notificationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(notification, unread2));
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        notificationService.markAllAsRead(10L);

        verify(notificationMapper, times(2)).updateById(any(Notification.class));
    }
}
