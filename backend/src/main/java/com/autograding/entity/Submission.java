package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "`submission`")
@TableName("submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @TableField(exist = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", insertable = false, updatable = false)
    private Assignment assignment;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @TableField(exist = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", insertable = false, updatable = false)
    private Problem problem;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @TableField(exist = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @Column(name = "code_content", nullable = false, columnDefinition = "LONGTEXT")
    private String codeContent;

    @Column(length = 20)
    private String language = "c";

    @Column(name = "submit_count")
    private Integer submitCount = 1;

    @Column(name = "is_late")
    private Integer isLate = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @TableLogic
    @Column
    private Integer deleted;
}
