package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    public enum Type {
        ASSIGNMENT, GRADING, SYSTEM, ANNOUNCEMENT
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Type type;

    private Integer isRead = 0;

    private Long relatedId;

    private LocalDateTime createdAt;
}
