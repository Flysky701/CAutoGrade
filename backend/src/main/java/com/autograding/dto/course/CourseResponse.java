package com.autograding.dto.course;

import com.autograding.entity.Course;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseResponse {
    private Long id;
    private String name;
    private String description;
    private Long teacherId;
    private String teacherName;
    private String semester;
    private String coverUrl;
    private String inviteCode;
    private Course.Status status;
    private LocalDateTime createdAt;

    public static CourseResponse fromEntity(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setName(course.getName());
        response.setDescription(course.getDescription());
        response.setTeacherId(course.getTeacherId());
        response.setSemester(course.getSemester());
        response.setCoverUrl(course.getCoverUrl());
        response.setInviteCode(course.getInviteCode());
        response.setStatus(course.getStatus());
        response.setCreatedAt(course.getCreatedAt());
        if (course.getTeacher() != null) {
            response.setTeacherName(course.getTeacher().getNickname());
        }
        return response;
    }
}
