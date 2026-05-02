package com.autograding.mapper;

import com.autograding.entity.TestCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
}
