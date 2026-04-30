package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grading_result")
public class GradingResult {

    public enum GradingStatus {
        PENDING, PROCESSING, DONE, FAILED
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("submission_id")
    private Long submissionId;

    private BigDecimal totalScore;

    private BigDecimal correctnessScore;

    private BigDecimal styleScore;

    private BigDecimal efficiencyScore;

    private String feedbackJson;

    private String testCaseResult;

    private String staticAnalysisResult;

    private String llmRawResponse;

    private GradingStatus gradingStatus = GradingStatus.PENDING;

    private Long reviewedBy;

    private BigDecimal humanAdjustedScore;

    private String reviewFeedback;

    private LocalDateTime reviewedAt;

    private LocalDateTime gradedAt;
}
