package com.aji.accounting.auth;

import com.aji.accounting.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    @Order(1)
    void firstRegistrationBecomesActiveAdmin() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Admin Satu","email":"admin@demo.local","username":"admin",
                         "password":"Rahasia#123","confirmPassword":"Rahasia#123","companyName":"PT Demo Manufaktur"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @Order(2)
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Lemah","email":"lemah@demo.local","username":"lemah",
                         "password":"12345678","confirmPassword":"12345678"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @Order(3)
    void secondRegistrationIsPendingAndCannotLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Staf Dua","email":"staf@demo.local","username":"staf",
                         "password":"Rahasia#123","confirmPassword":"Rahasia#123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identifier":"staf","password":"Rahasia#123"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    void loginSetsCookiesAndMeReturnsPermissions() throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identifier":"admin@demo.local","password":"Rahasia#123","rememberMe":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn();

        Cookie access = res.getResponse().getCookie(AuthCookiesNames.ACCESS);
        Cookie refresh = res.getResponse().getCookie(AuthCookiesNames.REFRESH);
        assertThat(access).isNotNull();
        assertThat(refresh).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();

        mvc.perform(get("/api/v1/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));

        // refresh token rotation
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(refreshed.getResponse().getCookie(AuthCookiesNames.REFRESH).getValue())
                .isNotEqualTo(refresh.getValue());
    }

    @Test
    @Order(5)
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void accountLocksAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                            {"identifier":"admin","password":"salah"}
                            """))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identifier":"admin","password":"Rahasia#123"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("terkunci")));
    }

    /** Nama cookie diduplikasi agar test tidak bergantung pada kelas internal. */
    static final class AuthCookiesNames {
        static final String ACCESS = "access_token";
        static final String REFRESH = "refresh_token";
    }
}
