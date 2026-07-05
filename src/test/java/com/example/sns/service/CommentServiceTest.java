package com.example.sns.service;

import com.example.sns.dto.CommentCreateRequest;
import com.example.sns.dto.CommentResponse;
import com.example.sns.dto.CommentUpdateRequest;
import com.example.sns.entity.Comment;
import com.example.sns.entity.Post;
import com.example.sns.entity.User;
import com.example.sns.exception.CustomException;
import com.example.sns.repository.CommentRepository;
import com.example.sns.repository.PostRepository;
import com.example.sns.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글 작성에 성공한다")
    void createComment_success() {
        // Given
        Long userId = 1L;
        Long postId = 1L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", userId);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", postId);

        CommentCreateRequest request =
                new CommentCreateRequest(
                        postId,
                        "첫 번째 댓글입니다."
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(commentRepository.save(any(Comment.class)))
                .willAnswer(invocation -> {
                    Comment comment =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            comment,
                            "id",
                            1L
                    );

                    return comment;
                });

        // When
        CommentResponse response =
                commentService.createComment(
                        userId,
                        request
                );

        // Then
        assertAll(
                () -> assertEquals(
                        1L,
                        response.commentId()
                ),
                () -> assertEquals(
                        postId,
                        response.postId()
                ),
                () -> assertEquals(
                        userId,
                        response.writerId()
                ),
                () -> assertEquals(
                        "테스트유저",
                        response.writerNickname()
                ),
                () -> assertEquals(
                        "첫 번째 댓글입니다.",
                        response.content()
                )
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, times(1))
                .findById(postId);

        verify(commentRepository, times(1))
                .save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 댓글 작성 시 예외가 발생한다")
    void createComment_userNotFound() {
        // Given
        Long userId = 999L;
        Long postId = 1L;

        CommentCreateRequest request =
                new CommentCreateRequest(
                        postId,
                        "댓글입니다."
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> commentService.createComment(
                        userId,
                        request
                )
        );

        assertEquals(
                "존재하지 않는 사용자입니다.",
                exception.getMessage()
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, never())
                .findById(any());

        verify(commentRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외가 발생한다")
    void createComment_postNotFound() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", userId);

        CommentCreateRequest request =
                new CommentCreateRequest(
                        postId,
                        "댓글입니다."
                );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.findById(postId))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> commentService.createComment(
                        userId,
                        request
                )
        );

        assertEquals(
                "존재하지 않는 게시글입니다.",
                exception.getMessage()
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, times(1))
                .findById(postId);

        verify(commentRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("댓글 전체 조회에 성공한다")
    void getComments_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment1 =
                Comment.create(
                        "첫 번째 댓글",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment1,
                "id",
                1L
        );

        Comment comment2 =
                Comment.create(
                        "두 번째 댓글",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment2,
                "id",
                2L
        );

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Comment> commentPage =
                new PageImpl<>(
                        List.of(comment1, comment2),
                        pageable,
                        2
                );

        given(commentRepository.findAll(pageable))
                .willReturn(commentPage);

        // When
        Page<CommentResponse> responses =
                commentService.getComments(pageable);

        // Then
        assertEquals(
                2,
                responses.getContent().size()
        );

        assertEquals(
                "첫 번째 댓글",
                responses.getContent().get(0).content()
        );

        assertEquals(
                "두 번째 댓글",
                responses.getContent().get(1).content()
        );

        assertEquals(
                2,
                responses.getTotalElements()
        );

        assertEquals(
                1,
                responses.getTotalPages()
        );

        verify(commentRepository, times(1))
                .findAll(pageable);
    }

    @Test
    @DisplayName("특정 게시글의 댓글 조회에 성공한다")
    void getCommentsByPost_success() {
        // Given
        Long postId = 1L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", postId);

        Comment comment =
                Comment.create(
                        "게시글에 달린 댓글",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment,
                "id",
                1L
        );

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<Comment> commentPage =
                new PageImpl<>(
                        List.of(comment),
                        pageable,
                        1
                );

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(commentRepository.findByPostId(
                postId,
                pageable
        )).willReturn(commentPage);

        // When
        Page<CommentResponse> responses =
                commentService.getCommentsByPost(
                        postId,
                        pageable
                );

        // Then
        assertEquals(
                1,
                responses.getContent().size()
        );

        assertEquals(
                "게시글에 달린 댓글",
                responses.getContent().get(0).content()
        );

        assertEquals(
                1,
                responses.getTotalElements()
        );

        assertEquals(
                1,
                responses.getTotalPages()
        );

        verify(postRepository, times(1))
                .findById(postId);

        verify(commentRepository, times(1))
                .findByPostId(postId, pageable);
    }

    @Test
    @DisplayName("댓글 단건 조회에 성공한다")
    void getComment_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment =
                Comment.create(
                        "댓글입니다.",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment,
                "id",
                1L
        );

        given(commentRepository.findById(1L))
                .willReturn(Optional.of(comment));

        // When
        CommentResponse response =
                commentService.getComment(1L);

        // Then
        assertEquals(
                1L,
                response.commentId()
        );

        assertEquals(
                "댓글입니다.",
                response.content()
        );

        verify(commentRepository, times(1))
                .findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 조회 시 예외가 발생한다")
    void getComment_notFound() {
        // Given
        given(commentRepository.findById(999L))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> commentService.getComment(999L)
        );

        assertEquals(
                "존재하지 않는 댓글입니다.",
                exception.getMessage()
        );

        verify(commentRepository, times(1))
                .findById(999L);
    }

    @Test
    @DisplayName("댓글 수정에 성공한다")
    void updateComment_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment =
                Comment.create(
                        "기존 댓글",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment,
                "id",
                1L
        );

        CommentUpdateRequest request =
                new CommentUpdateRequest(
                        "수정된 댓글"
                );

        given(commentRepository.findById(1L))
                .willReturn(Optional.of(comment));

        // When
        CommentResponse response =
                commentService.updateComment(
                        1L,
                        request
                );

        // Then
        assertEquals(
                "수정된 댓글",
                response.content()
        );

        verify(commentRepository, times(1))
                .findById(1L);
    }

    @Test
    @DisplayName("댓글 삭제에 성공한다")
    void deleteComment_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );
        ReflectionTestUtils.setField(post, "id", 1L);

        Comment comment =
                Comment.create(
                        "삭제할 댓글",
                        user,
                        post
                );
        ReflectionTestUtils.setField(
                comment,
                "id",
                1L
        );

        given(commentRepository.findById(1L))
                .willReturn(Optional.of(comment));

        // When
        commentService.deleteComment(1L);

        // Then
        verify(commentRepository, times(1))
                .findById(1L);

        verify(commentRepository, times(1))
                .delete(comment);
    }
}