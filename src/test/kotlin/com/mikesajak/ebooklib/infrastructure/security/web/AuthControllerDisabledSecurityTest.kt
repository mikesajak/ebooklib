package com.mikesajak.ebooklib.infrastructure.security.web

import com.mikesajak.ebooklib.infrastructure.security.SecurityConfig
import com.mikesajak.ebooklib.infrastructure.security.UserService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
@org.springframework.test.context.TestPropertySource(properties = ["app.security.enabled=false"])
class AuthControllerDisabledSecurityTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @org.springframework.boot.test.mock.mockito.MockBean
    private lateinit var userService: UserService

    @Test
    fun `should return mock user info when security is disabled`() {
        mockMvc.get("/api/me")
            .andExpect {
                status { isOk() }
                jsonPath("$.username") { value("dev-user") }
                jsonPath("$.roles") { value(org.hamcrest.Matchers.containsInAnyOrder("ROLE_ADMIN", "ROLE_USER")) }
            }
    }
}
