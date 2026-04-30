package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.TestCase;
import com.autograding.mapper.TestCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestCaseService {

    private final TestCaseMapper testCaseMapper;

    public TestCaseService(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    public TestCase createTestCase(TestCase testCase) {
        testCase.setCreatedAt(LocalDateTime.now());
        testCase.setUpdatedAt(LocalDateTime.now());
        testCase.setDeleted(0);
        testCaseMapper.insert(testCase);
        return testCase;
    }

    public List<TestCase> getTestCasesByProblem(Long problemId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getProblemId, problemId)
               .eq(TestCase::getDeleted, 0)
               .orderByAsc(TestCase::getSortOrder);
        return testCaseMapper.selectList(wrapper);
    }

    public List<TestCase> getVisibleTestCases(Long problemId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestCase::getProblemId, problemId)
               .eq(TestCase::getDeleted, 0)
               .eq(TestCase::getIsHidden, 0)
               .orderByAsc(TestCase::getSortOrder);
        return testCaseMapper.selectList(wrapper);
    }

    public TestCase getTestCaseById(Long id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null || testCase.getDeleted() == 1) {
            throw new BusinessException("测试用例不存在");
        }
        return testCase;
    }

    public TestCase updateTestCase(Long id, TestCase request) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null || testCase.getDeleted() == 1) {
            throw new BusinessException("测试用例不存在");
        }

        LambdaUpdateWrapper<TestCase> wrapper = new LambdaUpdateWrapper<TestCase>()
                .eq(TestCase::getId, id)
                .set(request.getInputData() != null, TestCase::getInputData, request.getInputData())
                .set(request.getExpectedOutput() != null, TestCase::getExpectedOutput, request.getExpectedOutput())
                .set(request.getIsHidden() != null, TestCase::getIsHidden, request.getIsHidden())
                .set(request.getWeight() != null, TestCase::getWeight, request.getWeight())
                .set(request.getSortOrder() != null, TestCase::getSortOrder, request.getSortOrder())
                .set(TestCase::getUpdatedAt, LocalDateTime.now());
        testCaseMapper.update(null, wrapper);
        return getTestCaseById(id);
    }

    public void deleteTestCase(Long id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null || testCase.getDeleted() == 1) {
            throw new BusinessException("测试用例不存在");
        }

        LambdaUpdateWrapper<TestCase> wrapper = new LambdaUpdateWrapper<TestCase>()
                .eq(TestCase::getId, id)
                .set(TestCase::getDeleted, 1)
                .set(TestCase::getUpdatedAt, LocalDateTime.now());
        testCaseMapper.update(null, wrapper);
    }
}
