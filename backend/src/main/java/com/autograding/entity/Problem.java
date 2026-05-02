package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("problem")
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Integer difficulty;

    private String inputDesc;

    private String outputDesc;

    private Integer timeLimitMs = 1000;

    private Integer memoryLimitKb = 256000;

    private String knowledgeTags;

    private Long creatorId;

    private Integer isPublic = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
