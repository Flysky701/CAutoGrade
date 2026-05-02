package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.Problem;
import com.autograding.entity.User;
import com.autograding.mapper.ProblemMapper;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemMapper problemMapper;
    @Mock private UserMapper userMapper;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private ProblemService problemService;

    private User teacher;
    private Problem problem;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setRole(User.Role.TEACHER);
        teacher.setDeleted(0);

        problem = new Problem();
        problem.setId(100L);
        problem.setTitle("计算两数之和");
        problem.setDescription("读取两个整数，输出它们的和");
        problem.setCreatorId(1L);
        problem.setDifficulty(1);
        problem.setIsPublic(1);
        problem.setDeleted(0);

        var auth = new UsernamePasswordAuthenticationToken(
            new org.springframework.security.core.userdetails.User("teacher1", "", List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))),
            null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProblem_shouldSucceed() {
        lenient().when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(teacher);
        when(userMapper.selectById(1L)).thenReturn(teacher);
        when(problemMapper.insert(any(Problem.class))).thenReturn(1);

        Problem newProblem = new Problem();
        newProblem.setTitle("计算两数之和");
        newProblem.setDifficulty(1);

        Problem result = problemService.createProblem(newProblem, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getCreatorId());
        verify(problemMapper).insert(newProblem);
    }

    @Test
    void createProblem_shouldThrowWhenCreatorNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        Problem newProblem = new Problem();
        newProblem.setTitle("计算两数之和");

        assertThrows(BusinessException.class,
                () -> problemService.createProblem(newProblem, 99L));
    }

    @Test
    void getProblemsByCreator_shouldReturnList() {
        when(problemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(problem));

        List<Problem> results = problemService.getProblemsByCreator(1L);

        assertEquals(1, results.size());
        assertEquals("计算两数之和", results.get(0).getTitle());
    }

    @Test
    void getPublicProblems_shouldReturnList() {
        when(problemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(problem));

        List<Problem> results = problemService.getPublicProblems();

        assertEquals(1, results.size());
    }

    @Test
    void getProblemById_shouldSucceed() {
        when(problemMapper.selectById(100L)).thenReturn(problem);

        Problem result = problemService.getProblemById(100L);

        assertEquals("计算两数之和", result.getTitle());
    }

    @Test
    void getProblemById_shouldThrowWhenNotFound() {
        when(problemMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> problemService.getProblemById(99L));
    }

    @Test
    void updateProblem_shouldThrowWhenNotCreator() {
        when(problemMapper.selectById(100L)).thenReturn(problem);

        Problem update = new Problem();
        update.setTitle("修改后的标题");

        assertThrows(BusinessException.class,
                () -> problemService.updateProblem(100L, update, 99L));
    }

    @Test
    void deleteProblem_shouldThrowWhenNotCreator() {
        when(problemMapper.selectById(100L)).thenReturn(problem);

        assertThrows(BusinessException.class,
                () -> problemService.deleteProblem(100L, 99L));
    }
}
