package com.example.webrtcbackend.courses.dto;

import com.example.webrtcbackend.courses.entity.CourseLevel;

public class CourseDTO {

    private String title;

    private String description;

    private CourseLevel level;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CourseDTO(String title, String description, CourseLevel level) {
        this.title = title;
        this.description = description;
        this.level = level;
    }

    public CourseDTO() {
    }

    public CourseLevel getLevel() {
        return level;
    }

    public void setLevel(CourseLevel level) {
        this.level = level;
    }
}
