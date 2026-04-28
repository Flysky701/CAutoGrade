package com.autograding.service;

import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradingService {

    private final SubmissionMapper submissionMapper;
    private final GradingResultMapper gradingResultMapper;

    public GradingService(SubmissionMapper submissionMapper, GradingResultMapper gradingResultMapper) {
        this.submissionMapper = submissionMapper;
        this.gradingResultMapper = gradingResultMapper;
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

    public List<GradingResult> getUnreviewedGradings() {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(GradingResult::getReviewedBy)
              .eq(GradingResult::getGradingStatus, GradingResult.GradingStatus.DONE);
        return gradingResultMapper.selectList(wrapper);
    }

    public GradingResult getGradingResultBySubmissionId(Long submissionId) {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getSubmissionId, submissionId);
        return gradingResultMapper.selectOne(wrapper);
    }
}
