package com.autograding.service;

import com.autograding.entity.GradingResult;
import com.autograding.entity.Problem;
import com.autograding.entity.Submission;
import com.autograding.entity.User;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.ProblemMapper;
import com.autograding.mapper.SubmissionMapper;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GradingService {

    private final SubmissionMapper submissionMapper;
    private final GradingResultMapper gradingResultMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;

    public GradingService(SubmissionMapper submissionMapper,
                          GradingResultMapper gradingResultMapper,
                          UserMapper userMapper,
                          ProblemMapper problemMapper) {
        this.submissionMapper = submissionMapper;
        this.gradingResultMapper = gradingResultMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
    }

    public List<GradingResult> getPendingGradings() {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getGradingStatus, GradingResult.GradingStatus.PENDING);
        return gradingResultMapper.selectList(wrapper);
    }

    public List<GradingResult> getGradingsByAssignment(Long assignmentId) {
        LambdaQueryWrapper<Submission> subWrapper = new LambdaQueryWrapper<>();
        subWrapper.eq(Submission::getAssignmentId, assignmentId);
        List<Submission> submissions = submissionMapper.selectList(subWrapper);

        if (submissions.isEmpty()) {
            return List.of();
        }

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();

        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GradingResult::getSubmissionId, submissionIds);
        return gradingResultMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getUnreviewedGradings() {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(GradingResult::getReviewedBy)
              .in(GradingResult::getGradingStatus,
                      GradingResult.GradingStatus.DONE,
                      GradingResult.GradingStatus.FAILED);
        List<GradingResult> results = gradingResultMapper.selectList(wrapper);

        List<Map<String, Object>> enriched = new ArrayList<>();
        for (GradingResult gr : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", gr.getId());
            item.put("submissionId", gr.getSubmissionId());
            item.put("totalScore", gr.getTotalScore());
            item.put("correctnessScore", gr.getCorrectnessScore());
            item.put("styleScore", gr.getStyleScore());
            item.put("efficiencyScore", gr.getEfficiencyScore());
            item.put("gradingStatus", gr.getGradingStatus() != null ? gr.getGradingStatus().name() : null);
            item.put("feedbackJson", gr.getFeedbackJson());
            item.put("testCaseResult", gr.getTestCaseResult());
            item.put("gradedAt", gr.getGradedAt());

            if (gr.getSubmissionId() != null) {
                Submission sub = submissionMapper.selectById(gr.getSubmissionId());
                if (sub != null) {
                    item.put("studentId", sub.getStudentId());
                    item.put("problemId", sub.getProblemId());
                    item.put("assignmentId", sub.getAssignmentId());
                    if (sub.getStudentId() != null) {
                        User student = userMapper.selectById(sub.getStudentId());
                        if (student != null) {
                            item.put("studentName", student.getNickname());
                            item.put("username", student.getUsername());
                        }
                    }
                    if (sub.getProblemId() != null) {
                        Problem problem = problemMapper.selectById(sub.getProblemId());
                        if (problem != null) {
                            item.put("problemTitle", problem.getTitle());
                        }
                    }
                }
            }
            enriched.add(item);
        }
        return enriched;
    }

    public GradingResult getGradingResultBySubmissionId(Long submissionId) {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getSubmissionId, submissionId)
               .last("limit 1");
        return gradingResultMapper.selectOne(wrapper);
    }
}
