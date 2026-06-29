package com.example.sns.auth;

import com.example.sns.exception.CustomException;
import com.example.sns.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    // 컨트롤러 실행 전에 JWT 토큰을 검사하는 메서드
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String token = resolveToken(request);

        // 토큰이 없는 경우
        if (token == null || token.isBlank()) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 토큰이 유효하지 않은 경우
        if (!jwtUtil.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰에서 userId와 email 추출
        Long userId = jwtUtil.getUserId(token);
        String email = jwtUtil.getEmail(token);

        // 이후 컨트롤러에서 사용할 수 있도록 request에 저장
        request.setAttribute("userId", userId);
        request.setAttribute("email", email);

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        // Postman/API 요청: Authorization 헤더에서 토큰 추출
        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        // Authorization 헤더가 있는데 Bearer 형식이 아닌 경우
        if (authorizationHeader != null
                && !authorizationHeader.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 브라우저/Thymeleaf 요청: 쿠키에서 토큰 추출
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}