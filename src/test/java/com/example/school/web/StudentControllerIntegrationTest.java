package com.example.school.web;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentControllerIntegrationTest {

    private static final String TEACHER_USERNAME = "test_teacher";
    private static final String STUDENT_USERNAME = "test_student";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createTestUsers();
    }

    private void createTestUsers() {
        User teacher = new User();
        teacher.setUsername(TEACHER_USERNAME);
        teacher.setPassword(passwordEncoder.encode("password"));
        teacher.setName("Test Teacher");
        teacher.setRole(Role.TEACHER);
        userRepository.save(teacher);

        User student = new User();
        student.setUsername(STUDENT_USERNAME);
        student.setPassword(passwordEncoder.encode("password"));
        student.setName("Test Student");
        student.setRole(Role.STUDENT);
        userRepository.save(student);
    }

    @Test
    @DisplayName("GET /api/students unauthenticated returns 401")
    void listStudents_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/students as teacher returns 200")
    @WithUserDetails(TEACHER_USERNAME)
    void listStudents_asTeacher_returns200() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/students/me as student returns own profile")
    @WithUserDetails(STUDENT_USERNAME)
    void getMe_asStudent_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/students/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(STUDENT_USERNAME));
    }

    @Test
    @DisplayName("POST /api/students as teacher creates student")
    @WithUserDetails(TEACHER_USERNAME)
    void createStudent_asTeacher_createsStudent() throws Exception {
        String body = """
                {"username":"newstudent1","password":"pass123","name":"New Student"}
                """;
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newstudent1"))
                .andExpect(jsonPath("$.name").value("New Student"));
    }

    @Test
    @DisplayName("POST /api/students as student returns 400")
    @WithUserDetails(STUDENT_USERNAME)
    void createStudent_asStudent_returns400() throws Exception {
        String body = "{\"username\":\"x\",\"password\":\"y\",\"name\":\"X\"}";
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
