package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_case")
public class TestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long problemId;

    private String inputData;

    private String expectedOutput;

    private Integer isHidden = 1;

    private Integer weight = 10;

    private Integer sortOrder = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
