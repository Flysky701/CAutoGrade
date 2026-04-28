package com.autograding.dto.course;

import lombok.Data;

@Data
public class ClassCreateRequest {
    private String name;
    private Long courseId;
}
