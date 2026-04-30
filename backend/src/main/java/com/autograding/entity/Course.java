package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {

    public enum Status {
        ACTIVE,
        ARCHIVED
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long teacherId;

    private String semester;

    private String coverUrl;

    private Status status = Status.ACTIVE;

    @TableField("created_at")
    private LocalDateTime createdAt;

    private String inviteCode;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
