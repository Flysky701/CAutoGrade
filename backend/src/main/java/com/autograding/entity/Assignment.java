package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assignment")
public class Assignment {

    public enum Type {
        EXAM, LAB, PRACTICE
    }

    public enum Status {
        DRAFT, PUBLISHED, EXPIRED, ARCHIVED
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Long courseId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer maxScore = 100;

    private Type type = Type.PRACTICE;

    private Status status = Status.DRAFT;

    private Integer latePenaltyScore = 0;

    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
