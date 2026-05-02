package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.TestCase;
import com.autograding.mapper.TestCaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    @Mock private TestCaseMapper testCaseMapper;

    @InjectMocks
    private TestCaseService testCaseService;

    private TestCase testCase;
    private TestCase hiddenCase;

    @BeforeEach
    void setUp() {
        testCase = new TestCase();
        testCase.setId(1L);
        testCase.setProblemId(100L);
        testCase.setInputData("1 2");
        testCase.setExpectedOutput("3");
        testCase.setIsHidden(0);
        testCase.setWeight(5);
        testCase.setSortOrder(0);
        testCase.setDeleted(0);

        hiddenCase = new TestCase();
        hiddenCase.setId(2L);
        hiddenCase.setProblemId(100L);
        hiddenCase.setInputData("100 200");
        hiddenCase.setExpectedOutput("300");
        hiddenCase.setIsHidden(1);
        hiddenCase.setDeleted(0);
    }

    @Test
    void createTestCase_shouldSucceed() {
        when(testCaseMapper.insert(any(TestCase.class))).thenReturn(1);

        TestCase result = testCaseService.createTestCase(testCase);

        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
        verify(testCaseMapper).insert(testCase);
    }

    @Test
    void getTestCasesByProblem_shouldReturnAll() {
        when(testCaseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testCase, hiddenCase));

        List<TestCase> results = testCaseService.getTestCasesByProblem(100L);

        assertEquals(2, results.size());
    }

    @Test
    void getVisibleTestCases_shouldExcludeHidden() {
        when(testCaseMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testCase));

        List<TestCase> results = testCaseService.getVisibleTestCases(100L);

        assertEquals(1, results.size());
        assertEquals(0, results.get(0).getIsHidden());
    }

    @Test
    void getTestCaseById_shouldSucceed() {
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        TestCase result = testCaseService.getTestCaseById(1L);

        assertEquals("1 2", result.getInputData());
        assertEquals("3", result.getExpectedOutput());
    }

    @Test
    void getTestCaseById_shouldThrowWhenNotFound() {
        when(testCaseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> testCaseService.getTestCaseById(99L));
    }

    @Test
    void updateTestCase_shouldThrowWhenNotFound() {
        when(testCaseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> testCaseService.updateTestCase(99L, new TestCase()));
    }

    @Test
    void deleteTestCase_shouldThrowWhenNotFound() {
        when(testCaseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> testCaseService.deleteTestCase(99L));
    }
}
