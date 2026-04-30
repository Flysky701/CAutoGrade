package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Problem;
import com.autograding.entity.User;
import com.autograding.mapper.ProblemMapper;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProblemService {

    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProblemService(ProblemMapper problemMapper, UserMapper userMapper, OperationLogService operationLogService) {
        this.problemMapper = problemMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
    }

    public Problem createProblem(Problem problem, Long creatorId) {
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException("无效的创建者ID");
        }

        normalizeKnowledgeTags(problem);
        problem.setCreatorId(creatorId);
        if (problem.getIsPublic() == null) {
            problem.setIsPublic(0);
        }
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        problemMapper.insert(problem);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "CREATE_PROBLEM", "PROBLEM", problem.getId(),
                "创建题目: " + problem.getTitle(), null);
        return problem;
    }

    private void normalizeKnowledgeTags(Problem problem) {
        String tags = problem.getKnowledgeTags();
        if (tags == null || tags.isEmpty()) {
            problem.setKnowledgeTags("[]");
            return;
        }
        // If already a JSON array string, keep it as-is
        String trimmed = tags.trim();
        if (trimmed.startsWith("[")) return;
        // Comma-separated plain text → JSON array
        try {
            String[] parts = trimmed.split("[,，]");
            List<String> list = new java.util.ArrayList<>();
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) list.add(t);
            }
            problem.setKnowledgeTags(objectMapper.writeValueAsString(list));
        } catch (JsonProcessingException e) {
            problem.setKnowledgeTags("[]");
        }
    }

    public List<Problem> getProblemsByCreator(Long creatorId) {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getCreatorId, creatorId)
               .eq(Problem::getDeleted, 0)
               .orderByDesc(Problem::getCreatedAt);
        return problemMapper.selectList(wrapper);
    }

    public List<Problem> getPublicProblems() {
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Problem::getIsPublic, 1)
               .eq(Problem::getDeleted, 0)
               .orderByDesc(Problem::getCreatedAt);
        return problemMapper.selectList(wrapper);
    }

    public Problem getProblemById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || problem.getDeleted() == 1) {
            throw new BusinessException("题目不存在");
        }
        return problem;
    }

    public Problem updateProblem(Long id, Problem request, Long creatorId) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || problem.getDeleted() == 1) {
            throw new BusinessException("题目不存在");
        }
        if (!problem.getCreatorId().equals(creatorId)) {
            throw new BusinessException("无权限修改此题目");
        }

        normalizeKnowledgeTags(request);
        LambdaUpdateWrapper<Problem> wrapper = new LambdaUpdateWrapper<Problem>()
                .eq(Problem::getId, id)
                .set(request.getTitle() != null, Problem::getTitle, request.getTitle())
                .set(request.getDescription() != null, Problem::getDescription, request.getDescription())
                .set(request.getDifficulty() != null, Problem::getDifficulty, request.getDifficulty())
                .set(request.getInputDesc() != null, Problem::getInputDesc, request.getInputDesc())
                .set(request.getOutputDesc() != null, Problem::getOutputDesc, request.getOutputDesc())
                .set(request.getTimeLimitMs() != null, Problem::getTimeLimitMs, request.getTimeLimitMs())
                .set(request.getMemoryLimitKb() != null, Problem::getMemoryLimitKb, request.getMemoryLimitKb())
                .set(request.getKnowledgeTags() != null, Problem::getKnowledgeTags, request.getKnowledgeTags())
                .set(request.getIsPublic() != null, Problem::getIsPublic, request.getIsPublic())
                .set(Problem::getUpdatedAt, LocalDateTime.now());
        problemMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_PROBLEM", "PROBLEM", id,
                "更新题目", null);
        return getProblemById(id);
    }

    public void deleteProblem(Long id, Long creatorId) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || problem.getDeleted() == 1) {
            throw new BusinessException("题目不存在");
        }
        if (!problem.getCreatorId().equals(creatorId)) {
            throw new BusinessException("无权限删除此题目");
        }

        LambdaUpdateWrapper<Problem> wrapper = new LambdaUpdateWrapper<Problem>()
                .eq(Problem::getId, id)
                .set(Problem::getDeleted, 1)
                .set(Problem::getUpdatedAt, LocalDateTime.now());
        problemMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "DELETE_PROBLEM", "PROBLEM", id,
                "删除题目", null);
    }
}
