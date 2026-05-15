package com.example.webrtcbackend.courses.dto;

import com.example.webrtcbackend.courses.entity.CourseLevel;
import com.example.webrtcbackend.courses.entity.Courses;

import java.time.Instant;

/**
 * DTO that enriches the base Course entity with additional computed fields:
 * - lessonCount: number of lessons in the course
 * - lecturerName: full name of the lecturer who owns the course
 */
public class CourseDetailDTO {

    private Long id;
    private String title;
    private String description;
    private CourseLevel level;
    private Long lecturerId;
    private Instant createdAt;
    private Instant updatedAt;
    private int lessonCount;
    private String lecturerName;

    public CourseDetailDTO() {}

    public CourseDetailDTO(Courses course, int lessonCount, String lecturerName) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.level = course.getLevel();
        this.lecturerId = course.getLecturerId();
        this.createdAt = course.getCreatedAt();
        this.updatedAt = course.getUpdatedAt();
        this.lessonCount = lessonCount;
        this.lecturerName = lecturerName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CourseLevel getLevel() { return level; }
    public void setLevel(CourseLevel level) { this.level = level; }

    public Long getLecturerId() { return lecturerId; }
    public void setLecturerId(Long lecturerId) { this.lecturerId = lecturerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getLessonCount() { return lessonCount; }
    public void setLessonCount(int lessonCount) { this.lessonCount = lessonCount; }

    public String getLecturerName() { return lecturerName; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }
}
