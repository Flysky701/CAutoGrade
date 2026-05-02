package com.autograding.mapper;

import com.autograding.entity.GradingResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GradingResultMapper extends BaseMapper<GradingResult> {
}
