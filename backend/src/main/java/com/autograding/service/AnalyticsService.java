package com.autograding.service;

import com.autograding.entity.Assignment;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private BigDecimal getEffectiveScore(GradingResult r) {
        return r.getHumanAdjustedScore() != null ? r.getHumanAdjustedScore() : r.getTotalScore();
    }

    public Map<String, Object> getClassAnalytics(Long classId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        List<ClassStudent> classStudents = classStudentMapper.selectList(csWrapper);
        int totalStudents = classStudents.size();

        if (totalStudents == 0) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("totalStudents", 0);
            empty.put("submissionRate", 0.0);
            empty.put("averageScore", 0.0);
            empty.put("passRate", 0.0);
            empty.put("excellentRate", 0.0);
            empty.put("scoreDistribution", new LinkedHashMap<>());
            empty.put("knowledgePoints", List.of());
            empty.put("errorTop10", List.of());
            return empty;
        }

        List<Long> studentIds = classStudents.stream()
                .map(ClassStudent::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        if (!studentIds.isEmpty()) {
            subWrapper.in(Submission::getStudentId, studentIds);
        } else {
            subWrapper.eq(Submission::getStudentId, 0L);
        }
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int submittedCount = submissions.stream()
                .map(Submission::getStudentId)
                .collect(Collectors.toSet())
                .size();

        double submissionRate = (double) submittedCount / totalStudents * 100;

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<GradingResult> results;
        if (submissionIds.isEmpty()) {
            results = List.of();
        } else {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            grWrapper.in(GradingResult::getSubmissionId, submissionIds);
            results = gradingResultMapper.selectList(grWrapper);
        }
        double averageScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        long passedCount = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .filter(s -> s.compareTo(BigDecimal.valueOf(60)) >= 0)
                .count();
        double passRate = results.isEmpty() ? 0.0 : (double) passedCount / results.size() * 100;

        long excellentCount = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .filter(s -> s.compareTo(BigDecimal.valueOf(90)) >= 0)
                .count();
        double excellentRate = results.isEmpty() ? 0.0 : (double) excellentCount / results.size() * 100;

        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
        scoreDistribution.put("0-59", 0);
        scoreDistribution.put("60-69", 0);
        scoreDistribution.put("70-79", 0);
        scoreDistribution.put("80-89", 0);
        scoreDistribution.put("90-100", 0);
        for (GradingResult r : results) {
            BigDecimal effective = getEffectiveScore(r);
            if (effective == null) continue;
            int score = effective.intValue();
            if (score < 60) scoreDistribution.merge("0-59", 1, Integer::sum);
            else if (score < 70) scoreDistribution.merge("60-69", 1, Integer::sum);
            else if (score < 80) scoreDistribution.merge("70-79", 1, Integer::sum);
            else if (score < 90) scoreDistribution.merge("80-89", 1, Integer::sum);
            else scoreDistribution.merge("90-100", 1, Integer::sum);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalStudents", totalStudents);
        map.put("submissionRate", Math.round(submissionRate * 100) / 100.0);
        map.put("averageScore", Math.round(averageScore * 100) / 100.0);
        map.put("passRate", Math.round(passRate * 100) / 100.0);
        map.put("excellentRate", Math.round(excellentRate * 100) / 100.0);
        map.put("scoreDistribution", scoreDistribution);
        map.put("knowledgePoints", List.of());
        map.put("errorTop10", List.of());
        return map;
    }

    public Map<String, Object> getStudentAnalytics(Long studentId) {
        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getStudentId, studentId)
                 .eq(Submission::getDeleted, 0);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        int totalSubmissions = submissions.size();

        List<GradingResult> results = new ArrayList<>();
        if (!submissions.isEmpty()) {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
            grWrapper.in(GradingResult::getSubmissionId, submissionIds);
            results = gradingResultMapper.selectList(grWrapper);
        }

        double averageScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        BigDecimal maxScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
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

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<GradingResult> results = new ArrayList<>();
        if (!submissionIds.isEmpty()) {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            grWrapper.in(GradingResult::getSubmissionId, submissionIds);
            results = gradingResultMapper.selectList(grWrapper);
        }

        double averageScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        BigDecimal maxScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minScore = results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
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

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        List<GradingResult> results = new ArrayList<>();
        if (!submissionIds.isEmpty()) {
            LambdaQueryWrapper<GradingResult> grWrapper = new LambdaQueryWrapper<>();
            grWrapper.in(GradingResult::getSubmissionId, submissionIds);
            results = gradingResultMapper.selectList(grWrapper);
        }

        int passedCount = (int) results.stream()
                .map(this::getEffectiveScore)
                .filter(Objects::nonNull)
                .filter(s -> s.compareTo(BigDecimal.valueOf(60)) >= 0)
                .count();

        double passRate = totalSubmissions > 0 ? (double) passedCount / totalSubmissions * 100 : 0;

        return Map.of(
            "totalSubmissions", totalSubmissions,
            "passedCount", passedCount,
            "passRate", Math.round(passRate * 100) / 100.0
        );
    }
}
