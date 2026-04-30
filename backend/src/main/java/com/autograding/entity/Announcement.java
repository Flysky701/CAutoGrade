package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long publisherId;

    private String title;

    private String content;

    private Integer isPinned = 0;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
