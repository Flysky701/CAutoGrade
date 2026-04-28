package com.autograding.dto.course;

import lombok.Data;

@Data
public class CourseCreateRequest {
    private String name;
    private String description;
    private String semester;
    private String coverUrl;
}
