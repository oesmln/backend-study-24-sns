package com.example.sns.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        Long postId,

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content
) {
}