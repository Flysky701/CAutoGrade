package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "`grading_result`")
@TableName("grading_result")
public class GradingResult {

    public enum GradingStatus {
        PENDING, PROCESSING, DONE, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;

    @TableField(exist = false)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", insertable = false, updatable = false)
    private Submission submission;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "correctness_score", precision = 5, scale = 2)
    private BigDecimal correctnessScore;

    @Column(name = "style_score", precision = 5, scale = 2)
    private BigDecimal styleScore;

    @Column(name = "efficiency_score", precision = 5, scale = 2)
    private BigDecimal efficiencyScore;

    @Column(name = "feedback_json", columnDefinition = "JSON")
    private String feedbackJson;

    @Column(name = "test_case_result", columnDefinition = "JSON")
    private String testCaseResult;

    @Column(name = "static_analysis_result", columnDefinition = "JSON")
    private String staticAnalysisResult;

    @Column(name = "llm_raw_response", columnDefinition = "TEXT")
    private String llmRawResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", length = 20)
    private GradingStatus gradingStatus = GradingStatus.PENDING;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @TableField(exist = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", insertable = false, updatable = false)
    private User reviewer;

    @Column(name = "human_adjusted_score", precision = 5, scale = 2)
    private BigDecimal humanAdjustedScore;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
}
