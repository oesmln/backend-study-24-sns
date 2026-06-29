package com.example.sns.service;

import com.example.sns.auth.JwtUtil;
import com.example.sns.dto.LoginRequest;
import com.example.sns.dto.LoginResponse;
import com.example.sns.dto.TokenReissueRequest;
import com.example.sns.entity.RefreshToken;
import com.example.sns.entity.User;
import com.example.sns.exception.CustomException;
import com.example.sns.exception.ErrorCode;
import com.example.sns.repository.RefreshTokenRepository;
import com.example.sns.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = getUserByEmail(request.email());

        if (!user.getPassword().equals(request.password())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        String refreshToken = createRefreshToken();

        saveOrUpdateRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    // Access Token 재발급
    public LoginResponse reissueAccessToken(TokenReissueRequest request) {
        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(request.refreshToken())
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.REFRESH_TOKEN_NOT_FOUND
                                )
                        );

        User user = getUserById(refreshToken.getUserId());

        String newAccessToken = jwtUtil.createAccessToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(
                newAccessToken,
                refreshToken.getToken()
        );
    }

    // 이메일로 사용자 조회
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.EMAIL_NOT_FOUND)
                );
    }

    // ID로 사용자 조회
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    // Refresh Token 문자열 생성
    private String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // 기존 Refresh Token이 있으면 갱신하고, 없으면 새로 저장
    private void saveOrUpdateRefreshToken(Long userId, String token) {
        RefreshToken refreshToken =
                refreshTokenRepository.findByUserId(userId)
                        .map(existingToken -> {
                            existingToken.updateToken(token);
                            return existingToken;
                        })
                        .orElseGet(() ->
                                RefreshToken.create(userId, token)
                        );

        refreshTokenRepository.save(refreshToken);
    }
}