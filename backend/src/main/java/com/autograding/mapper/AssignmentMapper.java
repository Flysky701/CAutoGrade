package com.autograding.mapper;

import com.autograding.entity.Assignment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {
}
