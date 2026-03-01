package com.example.webrtcbackend.user;

import com.example.webrtcbackend.config.LiquibaseTestConfig;
import com.example.webrtcbackend.user.dto.UserCreateRequest;
import com.example.webrtcbackend.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig.class)
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Test
    void createUserStoresRoleAndEmail() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("lecturer1");
        request.setPassword("secret123");
        request.setFullName("Lecturer One");
        request.setEmail("lecturer1@example.com");
        request.setRole(UserRole.LECTURER);

        UserResponse response = userService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("lecturer1@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.LECTURER);
    }
}
