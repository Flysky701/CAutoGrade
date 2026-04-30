package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Assignment;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final GradingResultMapper gradingResultMapper;
    private final AssignmentMapper assignmentMapper;
    private final OperationLogService operationLogService;

    public SubmissionService(SubmissionMapper submissionMapper,
                            GradingResultMapper gradingResultMapper,
                            AssignmentMapper assignmentMapper,
                            OperationLogService operationLogService) {
        this.submissionMapper = submissionMapper;
        this.gradingResultMapper = gradingResultMapper;
        this.assignmentMapper = assignmentMapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public Submission submitCode(Long assignmentId, Long problemId, Long studentId, String code) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("作业不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(assignment.getEndTime());

        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getAssignmentId, assignmentId)
               .eq(Submission::getProblemId, problemId)
               .eq(Submission::getStudentId, studentId)
               .eq(Submission::getDeleted, 0);
        long submitCount = submissionMapper.selectCount(wrapper) + 1;

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setProblemId(problemId);
        submission.setStudentId(studentId);
        submission.setCodeContent(code);
        submission.setLanguage("c");
        submission.setSubmitCount((int) submitCount);
        submission.setIsLate(isLate ? 1 : 0);
        submission.setSubmittedAt(now);
        submissionMapper.insert(submission);

        GradingResult gradingResult = new GradingResult();
        gradingResult.setSubmissionId(submission.getId());
        gradingResult.setGradingStatus(GradingResult.GradingStatus.PENDING);
        gradingResultMapper.insert(gradingResult);

        operationLogService.logOperation(studentId, "SUBMIT_CODE", "SUBMISSION", submission.getId(),
                "提交代码 assignmentId=" + assignmentId + ", problemId=" + problemId, null);

        return submission;
    }

    public Submission getSubmissionById(Long id) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null || submission.getDeleted() == 1) {
            throw new BusinessException("提交记录不存在");
        }
        return submission;
    }

    public GradingResult getGradingResultBySubmission(Long submissionId) {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getSubmissionId, submissionId);
        return gradingResultMapper.selectOne(wrapper);
    }

    public GradingResult getGradingResultById(Long id) {
        GradingResult result = gradingResultMapper.selectById(id);
        if (result == null) {
            throw new BusinessException("批改结果不存在");
        }
        return result;
    }

    public void updateGradingResult(Long submissionId, GradingResult result) {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getSubmissionId, submissionId);
        GradingResult existing = gradingResultMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setTotalScore(result.getTotalScore());
            existing.setCorrectnessScore(result.getCorrectnessScore());
            existing.setStyleScore(result.getStyleScore());
            existing.setEfficiencyScore(result.getEfficiencyScore());
            existing.setFeedbackJson(result.getFeedbackJson());
            existing.setTestCaseResult(result.getTestCaseResult());
            existing.setStaticAnalysisResult(result.getStaticAnalysisResult());
            existing.setLlmRawResponse(result.getLlmRawResponse());
            existing.setGradingStatus(result.getGradingStatus());
            existing.setGradedAt(LocalDateTime.now());
            gradingResultMapper.updateById(existing);
        }
    }

    public void updateGradingStatus(Long submissionId, GradingResult.GradingStatus status) {
        LambdaQueryWrapper<GradingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GradingResult::getSubmissionId, submissionId);
        GradingResult existing = gradingResultMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setGradingStatus(status);
            gradingResultMapper.updateById(existing);
        }
    }

    public GradingResult reviewGrading(Long gradingId, Long teacherId, java.math.BigDecimal adjustedScore, String feedback) {
        GradingResult result = gradingResultMapper.selectById(gradingId);
        if (result == null) {
            throw new BusinessException("批改结果不存在");
        }

        result.setReviewedBy(teacherId);
        result.setHumanAdjustedScore(adjustedScore);
        result.setReviewedAt(LocalDateTime.now());
        if (feedback != null) {
            result.setFeedbackJson(feedback);
        }
        gradingResultMapper.updateById(result);
        operationLogService.logOperation(teacherId, "REVIEW_GRADING", "GRADING_RESULT", gradingId,
                "教师复核评分, adjustedScore=" + adjustedScore, null);
        return result;
    }

    public java.util.List<Submission> getSubmissionsByStudent(Long studentId) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getStudentId, studentId)
               .eq(Submission::getDeleted, 0)
               .orderByDesc(Submission::getSubmittedAt);
        return submissionMapper.selectList(wrapper);
    }

    public java.util.List<Submission> getSubmissionsByAssignment(Long assignmentId) {
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getAssignmentId, assignmentId)
               .eq(Submission::getDeleted, 0)
               .orderByDesc(Submission::getSubmittedAt);
        return submissionMapper.selectList(wrapper);
    }
}
