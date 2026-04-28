package com.autograding.service;

import com.autograding.entity.Assignment;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final SubmissionMapper submissionMapper;
    private final GradingResultMapper gradingResultMapper;
    private final AssignmentMapper assignmentMapper;
    private final CourseMapper courseMapper;
    private final ClassStudentMapper classStudentMapper;

    public AnalyticsService(SubmissionMapper submissionMapper,
                          GradingResultMapper gradingResultMapper,
                          AssignmentMapper assignmentMapper,
                          CourseMapper courseMapper,
                          ClassStudentMapper classStudentMapper) {
        this.submissionMapper = submissionMapper;
        this.gradingResultMapper = gradingResultMapper;
        this.assignmentMapper = assignmentMapper;
        this.courseMapper = courseMapper;
        this.classStudentMapper = classStudentMapper;
    }

    public Map<String, Object> getClassAnalytics(Long classId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        List<ClassStudent> classStudents = classStudentMapper.selectList(csWrapper);
        int totalStudents = classStudents.size();

        if (totalStudents == 0) {
            return Map.of(
                "totalStudents", 0,
                "submissionRate", 0.0,
                "averageScore", 0.0
            );
        }

        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getStudentId, 0L);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int submittedCount = submissions.stream()
                .map(Submission::getStudentId)
                .collect(Collectors.toSet())
                .size();

        double submissionRate = (double) submittedCount / totalStudents * 100;

        List<GradingResult> results = gradingResultMapper.selectList(null);
        double averageScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToDouble(r -> r.getTotalScore().doubleValue())
                .average()
                .orElse(0.0);

        return Map.of(
            "totalStudents", totalStudents,
            "submissionRate", Math.round(submissionRate * 100) / 100.0,
            "averageScore", Math.round(averageScore * 100) / 100.0
        );
    }

    public Map<String, Object> getStudentAnalytics(Long studentId) {
        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getStudentId, studentId)
                 .eq(Submission::getDeleted, 0);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int totalSubmissions = submissions.size();

        LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        grWrapper.in(GradingResult::getSubmissionId, submissionIds);
        List<GradingResult> results = gradingResultMapper.selectList(grWrapper);

        double averageScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToDouble(r -> r.getTotalScore().doubleValue())
                .average()
                .orElse(0.0);

        BigDecimal maxScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .map(GradingResult::getTotalScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return Map.of(
            "totalSubmissions", totalSubmissions,
            "averageScore", Math.round(averageScore * 100) / 100.0,
            "highestScore", maxScore
        );
    }

    public Map<String, Object> getAssignmentAnalytics(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            return Map.of();
        }

        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getAssignmentId, assignmentId)
                 .eq(Submission::getDeleted, 0);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int totalSubmissions = submissions.size();

        List<GradingResult> results = new ArrayList<>();
        for (Submission s : submissions) {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            grWrapper.eq(GradingResult::getSubmissionId, s.getId());
            GradingResult r = gradingResultMapper.selectOne(grWrapper);
            if (r != null) {
                results.add(r);
            }
        }

        double averageScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .mapToDouble(r -> r.getTotalScore().doubleValue())
                .average()
                .orElse(0.0);

        BigDecimal maxScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .map(GradingResult::getTotalScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minScore = results.stream()
                .filter(r -> r.getTotalScore() != null)
                .map(GradingResult::getTotalScore)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return Map.of(
            "totalSubmissions", totalSubmissions,
            "averageScore", Math.round(averageScore * 100) / 100.0,
            "highestScore", maxScore,
            "lowestScore", minScore,
            "maxScore", assignment.getMaxScore()
        );
    }

    public Map<String, Object> getProblemAnalytics(Long problemId) {
        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getProblemId, problemId)
                 .eq(Submission::getDeleted, 0);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int totalSubmissions = submissions.size();

        List<GradingResult> results = new ArrayList<>();
        for (Submission s : submissions) {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            grWrapper.eq(GradingResult::getSubmissionId, s.getId());
            GradingResult r = gradingResultMapper.selectOne(grWrapper);
            if (r != null) {
                results.add(r);
            }
        }

        int passedCount = (int) results.stream()
                .filter(r -> r.getTotalScore() != null && r.getTotalScore().compareTo(BigDecimal.valueOf(60)) >= 0)
                .count();

        double passRate = totalSubmissions > 0 ? (double) passedCount / totalSubmissions * 100 : 0;

        return Map.of(
            "totalSubmissions", totalSubmissions,
            "passedCount", passedCount,
            "passRate", Math.round(passRate * 100) / 100.0
        );
    }
}
