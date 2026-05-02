package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("assignment_problem")
public class AssignmentProblem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long problemId;

    private Integer sortOrder = 0;
}
