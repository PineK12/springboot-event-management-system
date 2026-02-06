package com.example.vadoo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage;

        if (exception instanceof UsernameNotFoundException) {
            errorMessage = "Tên đăng nhập không tồn tại";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Tài khoản đã bị vô hiệu hóa";
        } else if (exception instanceof LockedException) {
            errorMessage = "Tài khoản đã bị khóa";
        } else {
            errorMessage = "Đăng nhập thất bại. Vui lòng thử lại";
        }

        log.warn("Authentication failed: {}", errorMessage);

        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?error=" + encodedMessage);
    }
}