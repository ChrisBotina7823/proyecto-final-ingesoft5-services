package com.selimhorri.app.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

/**
 * Integration tests for UserResource
 * Tests full HTTP request/response cycle with real database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        CredentialDto credentialDto = CredentialDto.builder()
                .username("integration_test")
                .password("test123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
        
        testUserDto = UserDto.builder()
                .firstName("Integration")
                .lastName("Test")
                .email("integration@test.com")
                .phone("9876543210")
                .credentialDto(credentialDto)
                .build();
    }

    @Test
    void testGetAllUsers_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray());
    }

    @Test
    void testCreateUser_ShouldReturnCreatedUser() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUserDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Integration"))
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    void testGetUserById_ShouldReturnUser_WhenExists() throws Exception {
        // First create a user
        String userJson = objectMapper.writeValueAsString(testUserDto);
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andReturn().getResponse().getContentAsString();

        UserDto created = objectMapper.readValue(response, UserDto.class);

        // Then get it by ID
        mockMvc.perform(get("/api/users/" + created.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(created.getUserId()))
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    void testUpdateUser_ShouldReturnUpdatedUser() throws Exception {
        // First create a user
        String userJson = objectMapper.writeValueAsString(testUserDto);
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andReturn().getResponse().getContentAsString();

        UserDto created = objectMapper.readValue(response, UserDto.class);

        // Update the user
        created.setFirstName("Updated");
        String updatedJson = objectMapper.writeValueAsString(created);

        mockMvc.perform(put("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void testDeleteUser_ShouldReturnTrue() throws Exception {
        // First create a user
        String userJson = objectMapper.writeValueAsString(testUserDto);
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andReturn().getResponse().getContentAsString();

        UserDto created = objectMapper.readValue(response, UserDto.class);

        // Delete the user
        mockMvc.perform(delete("/api/users/" + created.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
