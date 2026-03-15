package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.courses.dto.CourseDTO;
import com.example.webrtcbackend.courses.entity.Courses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CoursesController {
    private final CourseService courseService;

    public CoursesController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Courses>> searchCourses(@RequestParam(value = "query", required = false) String query) {
        List<Courses> results = courseService.searchCourses(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Courses> findById(@PathVariable(value = "id", required = true) Long id) {
        Courses results = courseService.getCourseById(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/create")
    public ResponseEntity<Courses> create(@RequestBody CourseDTO request) {
        Courses results = courseService.createCourse(request.getTitle(), request.getDescription());
        return ResponseEntity.ok(results);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Courses> update(@RequestBody CourseDTO request, @PathVariable Long id) {
        Courses results = courseService.updateCourse(id, request.getTitle(), request.getDescription());
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }
}
