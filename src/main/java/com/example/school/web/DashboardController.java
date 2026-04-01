package com.example.school.web;

import com.example.school.entity.Role;
import com.example.school.entity.SchoolClass;
import com.example.school.entity.User;
import com.example.school.security.SchoolUserDetails;
import com.example.school.service.ClassService;
import com.example.school.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserService userService;
    private final ClassService classService;

    public DashboardController(UserService userService, ClassService classService) {
        this.userService = userService;
        this.classService = classService;
    }

    @GetMapping
    public ResponseEntity<?> dashboard() {
        Optional<SchoolUserDetails> current = userService.getCurrentUserDetails();
        if (current.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<User> user = userService.findById(current.get().getUserId());
        if (user.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User u = user.get();
        if (u.getRole() == Role.TEACHER) {
            List<SchoolClass> classes = classService.findClassesForCurrentUser(current.get());
            Map<Long, Long> counts = classService.enrollmentCountByClassId(classes);
            List<TeacherClassResponse> classSummaries = classes.stream()
                    .map(c -> TeacherClassResponse.from(c, counts.getOrDefault(c.getId(), 0L)))
                    .toList();
            return ResponseEntity.ok(new TeacherDashboardResponse(
                    UserResponse.from(u),
                    classSummaries
            ));
        }

        List<StudentEnrolledClassResponse> enrolled = classService.findEnrolledClassesForStudent(current.get()).stream()
                .map(StudentEnrolledClassResponse::from)
                .toList();

        return ResponseEntity.ok(new StudentDashboardResponse(
                UserResponse.from(u),
                enrolled
        ));
    }

    public record UserResponse(Long id, String username, String name, String email, String grade, String role) {
        static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getName(),
                    u.getEmail(),
                    u.getGrade(),
                    u.getRole() != null ? u.getRole().name() : null
            );
        }
    }

    public record TeacherDashboardResponse(UserResponse me, List<TeacherClassResponse> myClasses) {}

    public record TeacherClassResponse(Long id, String name, String description, Long enrollmentCount) {
        static TeacherClassResponse from(SchoolClass c, Long enrollmentCount) {
            return new TeacherClassResponse(c.getId(), c.getName(), c.getDescription(), enrollmentCount);
        }
    }

    public record StudentDashboardResponse(UserResponse me, List<StudentEnrolledClassResponse> enrolledClasses) {}

    public record StudentEnrolledClassResponse(Long id, String name, String description, String teacherName) {
        static StudentEnrolledClassResponse from(SchoolClass c) {
            return new StudentEnrolledClassResponse(
                    c.getId(),
                    c.getName(),
                    c.getDescription(),
                    c.getTeacher() != null ? c.getTeacher().getName() : null
            );
        }
    }
}

