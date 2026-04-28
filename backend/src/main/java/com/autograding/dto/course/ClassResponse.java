package com.autograding.dto.course;

import com.autograding.entity.Class;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassResponse {
    private Long id;
    private String name;
    private Long courseId;
    private String courseName;
    private String inviteCode;
    private LocalDateTime createdAt;
    private Integer studentCount;

    public static ClassResponse fromEntity(Class cls) {
        ClassResponse response = new ClassResponse();
        response.setId(cls.getId());
        response.setName(cls.getName());
        response.setCourseId(cls.getCourseId());
        response.setInviteCode(cls.getInviteCode());
        response.setCreatedAt(cls.getCreatedAt());
        if (cls.getCourse() != null) {
            response.setCourseName(cls.getCourse().getName());
        }
        return response;
    }
}
