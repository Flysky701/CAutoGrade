package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long problemId;

    private Long studentId;

    @TableField("code_content")
    private String codeContent;

    private String language = "c";

    private Integer submitCount = 1;

    private Integer isLate = 0;

    private LocalDateTime submittedAt;

    @TableLogic
    private Integer deleted;
}
