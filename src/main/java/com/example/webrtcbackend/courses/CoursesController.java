package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.auth.repository.AuthRepository;
import com.example.webrtcbackend.courses.dto.CourseDTO;
import com.example.webrtcbackend.courses.dto.CourseDetailDTO;
import com.example.webrtcbackend.courses.entity.Courses;
import com.example.webrtcbackend.user.User;
import com.example.webrtcbackend.user.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CoursesController {
    private final CourseService courseService;
    private final AuthRepository authRepository;

    public CoursesController(CourseService courseService, AuthRepository authRepository) {
        this.courseService = courseService;
        this.authRepository = authRepository;
    }

    /**
     * Search courses. For permitAll() endpoints, we manually resolve the user
     * from SecurityContextHolder because @AuthenticationPrincipal may be null
     * even when a valid JWT is present.
     */
    @GetMapping("/search")
    public ResponseEntity<List<CourseDetailDTO>> searchCourses(
            @RequestParam(value = "query", required = false) String query) {
        User user = resolveCurrentUser();
        if (user == null) {
            // Not authenticated — public access, return all courses
            List<CourseDetailDTO> results = courseService.searchCoursesWithDetails(query, null, UserRole.STUDENT);
            return ResponseEntity.ok(results);
        }
        List<CourseDetailDTO> results = courseService.searchCoursesWithDetails(query, user.getId(), user.getRole());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> findById(@PathVariable(value = "id") Long id) {
        CourseDetailDTO results = courseService.getCourseDetailById(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/create")
    public ResponseEntity<Courses> create(@RequestBody CourseDTO request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Courses results = courseService.createCourse(
                request.getTitle(), request.getDescription(), request.getLevel(), user.getId());
        return ResponseEntity.ok(results);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Courses> update(@RequestBody CourseDTO request, @PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Courses results = courseService.updateCourse(
                id, request.getTitle(), request.getDescription(), request.getLevel(),
                user.getId(), user.getRole());
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        courseService.deleteCourse(id, user.getId(), user.getRole());
    }

    /**
     * Resolve user from SecurityContextHolder — works for permitAll() endpoints.
     * Returns null if not authenticated.
     */
    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return authRepository.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    private User resolveUser(UserDetails userDetails) {
        return authRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
