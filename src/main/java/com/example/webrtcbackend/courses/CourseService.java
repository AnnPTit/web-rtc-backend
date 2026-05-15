package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.auth.repository.AuthRepository;
import com.example.webrtcbackend.courses.dto.CourseDetailDTO;
import com.example.webrtcbackend.courses.entity.CourseLevel;
import com.example.webrtcbackend.courses.entity.Courses;
import com.example.webrtcbackend.lessons.LessonRepository;
import com.example.webrtcbackend.user.User;
import com.example.webrtcbackend.user.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    private final CoursesRepository coursesRepository;
    private final LessonRepository lessonRepository;
    private final AuthRepository authRepository;

    public CourseService(CoursesRepository coursesRepository,
                         LessonRepository lessonRepository,
                         AuthRepository authRepository) {
        this.coursesRepository = coursesRepository;
        this.lessonRepository = lessonRepository;
        this.authRepository = authRepository;
    }

    /**
     * Create a course. The lecturerId is automatically set from the authenticated user.
     */
    public Courses createCourse(String title, String description, CourseLevel level, Long lecturerId) {
        Courses course = new Courses();
        course.setTitle(title);
        course.setDescription(description);
        course.setLevel(level != null ? level : CourseLevel.BEGINNER);
        course.setLecturerId(lecturerId);
        return coursesRepository.save(course);
    }

    public Courses getCourseById(Long id) {
        return coursesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    /**
     * Get a single course as CourseDetailDTO (with lessonCount and lecturerName).
     */
    public CourseDetailDTO getCourseDetailById(Long id) {
        Courses course = getCourseById(id);
        return toCourseDetailDTO(course);
    }

    /**
     * Update a course. LECTURER can only update their own courses. ADMIN can update any.
     */
    public Courses updateCourse(Long id, String title, String description, CourseLevel level,
                                Long currentUserId, UserRole currentUserRole) {
        Courses course = getCourseById(id);
        verifyOwnership(course, currentUserId, currentUserRole);
        course.setTitle(title);
        course.setDescription(description);
        if (level != null) {
            course.setLevel(level);
        }
        return coursesRepository.save(course);
    }

    /**
     * Delete a course. LECTURER can only delete their own courses. ADMIN can delete any.
     */
    public void deleteCourse(Long id, Long currentUserId, UserRole currentUserRole) {
        Courses course = getCourseById(id);
        verifyOwnership(course, currentUserId, currentUserRole);
        coursesRepository.delete(course);
    }

    /**
     * Search courses with role-based filtering:
     * - ADMIN: sees all courses
     * - LECTURER: sees only their own courses
     * - STUDENT: sees all courses (for browsing)
     */
    public List<Courses> searchCourses(String query, Long currentUserId, UserRole currentUserRole) {
        // LECTURER only sees their own courses
        if (currentUserRole == UserRole.LECTURER) {
            if (query == null || query.trim().isEmpty()) {
                return coursesRepository.findByLecturerId(currentUserId);
            }
            return coursesRepository.findByTitleContainingIgnoreCaseAndLecturerId(query, currentUserId);
        }

        // ADMIN and STUDENT see all courses
        if (query == null || query.trim().isEmpty()) {
            return coursesRepository.findAll();
        }
        return coursesRepository.findByTitleContainingIgnoreCase(query);
    }

    /**
     * Search courses and return as CourseDetailDTO (enriched with lessonCount and lecturerName).
     */
    public List<CourseDetailDTO> searchCoursesWithDetails(String query, Long currentUserId, UserRole currentUserRole) {
        List<Courses> courses = searchCourses(query, currentUserId, currentUserRole);
        return courses.stream()
                .map(this::toCourseDetailDTO)
                .toList();
    }

    /**
     * Convert a Courses entity to CourseDetailDTO by looking up lesson count and lecturer name.
     */
    private CourseDetailDTO toCourseDetailDTO(Courses course) {
        int lessonCount = (int) lessonRepository.countByCourseId(course.getId());
        String lecturerName = null;
        if (course.getLecturerId() != null) {
            lecturerName = authRepository.findById(course.getLecturerId())
                    .map(User::getFullName)
                    .orElse(null);
        }
        return new CourseDetailDTO(course, lessonCount, lecturerName);
    }

    /**
     * Verify that the current user owns the course, or is an ADMIN.
     */
    public void verifyOwnership(Courses course, Long currentUserId, UserRole currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            return; // ADMIN bypasses ownership check
        }
        if (course.getLecturerId() == null || !course.getLecturerId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên khóa học này");
        }
    }
}

