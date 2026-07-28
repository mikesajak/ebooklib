package com.mikesajak.ebooklib.infrastructure.security.web

import com.mikesajak.ebooklib.infrastructure.security.SecurityConfig
import com.mikesajak.ebooklib.infrastructure.security.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = ["app.security.enabled=true"])
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userService: UserService

    @Test
    @WithMockUser(username = "test-user", roles = ["USER"])
    fun `should return current user info when authenticated`() {
        mockMvc.get("/api/me") {
            with(csrf())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.username") { value("test-user") }
                jsonPath("$.roles") { value(org.hamcrest.Matchers.containsInAnyOrder("ROLE_USER")) }
            }
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.get("/api/me") {
            with(csrf())
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    @WithMockUser(username = "test-user", roles = ["USER"])
    fun `should change password when requested by authenticated user`() {
        mockMvc.post("/api/user/change-password") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = """{"currentPassword":"oldPass","newPassword":"newPass123"}"""
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("Password changed successfully") }
            }

        verify(userService).changePassword("test-user", "oldPass", "newPass123")
    }
}
