package com.autograding.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_shouldSetCode200AndMessage() {
        Result<String> result = Result.success("hello");

        assertEquals(200, result.getCode());
        assertEquals("成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void success_withNull_shouldReturnNullData() {
        Result<Void> result = Result.success(null);

        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void error_shouldSetCode500() {
        Result<Void> result = Result.error("出错了");

        assertEquals(500, result.getCode());
        assertEquals("出错了", result.getMsg());
        assertNull(result.getData());
    }
}
