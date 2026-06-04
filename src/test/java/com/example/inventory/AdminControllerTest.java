package com.example.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class AdminControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private String getToken(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Long getUserId(String username, String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var users = objectMapper.readTree(response);
        for (var user : users) {
            if (user.get("username").asText().equals(username)) {
                return user.get("id").asLong();
            }
        }
        throw new RuntimeException("User not found: " + username);
    }

    // ===== GET ALL USERS =====

    @Test
    void getAllUsers_asAdmin_returns200() throws Exception {
        String token = getToken("admin", "admin123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllUsers_asSupplier_returns403() throws Exception {
        String token = getToken("MadidonCo", "88005553535");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_asUser_returns403() throws Exception {
        String token = getToken("Madidon", "88005553535");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ===== UPDATE ROLE =====

    @Test
    void updateRole_asAdmin_returns200() throws Exception {
        String token = getToken("admin", "admin123");
        Long userId = getUserId("Madidon", token);
        String body = """
                {
                    "role": "ROLE_SUPPLIER"
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_SUPPLIER"));
    }

    @Test
    void updateRole_asSupplier_returns403() throws Exception {
        String adminToken = getToken("admin", "admin123");
        String supplierToken = getToken("MadidonCo", "88005553535");
        Long userId = getUserId("Madidon", adminToken);
        String body = """
                {
                    "role": "ROLE_USER"
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_asUser_returns403() throws Exception {
        String adminToken = getToken("admin", "admin123");
        String userToken = getToken("Madidon", "88005553535");
        Long userId = getUserId("Madidon", adminToken);
        String body = """
                {
                    "role": "ROLE_SUPPLIER"
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRole_nonExistingUser_returns404() throws Exception {
        String token = getToken("admin", "admin123");
        String body = """
            {
                "role": "ROLE_USER"
            }
            """;

        mockMvc.perform(put("/api/admin/users/99999/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_emptyRole_returns400() throws Exception {
        String token = getToken("admin", "admin123");
        Long userId = getUserId("Madidon", token);
        String body = """
                {
                    "role": ""
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_noToken_returns403() throws Exception {
        String body = """
                {
                    "role": "ROLE_USER"
                }
                """;

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // ===== DELETE USER =====

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        String token = getToken("admin", "admin123");
        Long userId = getUserId("Madidon", token);

        mockMvc.perform(delete("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_asSupplier_returns403() throws Exception {
        String adminToken = getToken("admin", "admin123");
        String supplierToken = getToken("MadidonCo", "88005553535");
        Long userId = getUserId("Madidon", adminToken);

        mockMvc.perform(delete("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asUser_returns403() throws Exception {
        String adminToken = getToken("admin", "admin123");
        String userToken = getToken("Madidon", "88005553535");
        Long userId = getUserId("Madidon", adminToken);

        mockMvc.perform(delete("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_noToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }
}