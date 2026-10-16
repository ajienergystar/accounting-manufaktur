package com.aji.accounting.auth;

import com.aji.accounting.auth.AuthDtos.ChangePasswordRequest;
import com.aji.accounting.auth.AuthDtos.ForgotPasswordRequest;
import com.aji.accounting.auth.AuthDtos.LoginRequest;
import com.aji.accounting.auth.AuthDtos.MeResponse;
import com.aji.accounting.auth.AuthDtos.RegisterRequest;
import com.aji.accounting.auth.AuthDtos.RegisterResponse;
import com.aji.accounting.auth.AuthDtos.RegistrationStatus;
import com.aji.accounting.auth.AuthDtos.ResetPasswordRequest;
import com.aji.accounting.common.web.MessageResponse;
import com.aji.accounting.config.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/registration-status")
    @Operation(summary = "Apakah sudah ada user (menentukan apakah form registrasi meminta nama perusahaan)")
    public RegistrationStatus registrationStatus() {
        return new RegistrationStatus(authService.hasUsers());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq,
                            HttpServletResponse httpRes) {
        return authService.login(req, httpReq, httpRes);
    }

    @PostMapping("/refresh")
    public MeResponse refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        return authService.refresh(httpReq, httpRes);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        authService.logout(httpReq, httpRes);
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me(CurrentUser.id());
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return MessageResponse.of("Jika email terdaftar, link reset password telah dikirim. Periksa kotak masuk Anda.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return MessageResponse.of("Password berhasil diubah. Silakan login dengan password baru.");
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest httpReq,
                                          HttpServletResponse httpRes) {
        authService.changePassword(CurrentUser.id(), req, httpReq, httpRes);
        return MessageResponse.of("Password berhasil diubah.");
    }
}
