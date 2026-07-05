package com.example.sns.service;

import com.example.sns.dto.LikeCountResponse;
import com.example.sns.dto.LikeCreateRequest;
import com.example.sns.dto.LikeResponse;
import com.example.sns.entity.Like;
import com.example.sns.entity.Post;
import com.example.sns.entity.User;
import com.example.sns.exception.CustomException;
import com.example.sns.repository.LikeRepository;
import com.example.sns.repository.PostRepository;
import com.example.sns.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private LikeService likeService;

    @Test
    @DisplayName("좋아요 생성에 성공한다")
    void createLike_success() {
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

        LikeCreateRequest request =
                new LikeCreateRequest(postId);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(likeRepository.existsByUserIdAndPostId(userId, postId))
                .willReturn(false);

        given(likeRepository.save(any(Like.class)))
                .willAnswer(invocation -> {
                    Like like = invocation.getArgument(0);
                    ReflectionTestUtils.setField(like, "id", 1L);
                    return like;
                });

        // When
        LikeResponse response =
                likeService.createLike(userId, request);

        // Then
        assertAll(
                () -> assertEquals(1L, response.likeId()),
                () -> assertEquals(postId, response.postId()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals(
                        "테스트유저",
                        response.userNickname()
                )
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, times(1))
                .findById(postId);

        verify(likeRepository, times(1))
                .existsByUserIdAndPostId(userId, postId);

        verify(likeRepository, times(1))
                .save(any(Like.class));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 게시글이면 예외가 발생한다")
    void createLike_alreadyLiked() {
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

        LikeCreateRequest request =
                new LikeCreateRequest(postId);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(likeRepository.existsByUserIdAndPostId(userId, postId))
                .willReturn(true);

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> likeService.createLike(
                        userId,
                        request
                )
        );

        assertEquals(
                "이미 좋아요를 누른 게시글입니다.",
                exception.getMessage()
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, times(1))
                .findById(postId);

        verify(likeRepository, times(1))
                .existsByUserIdAndPostId(userId, postId);

        verify(likeRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 좋아요 생성 시 예외가 발생한다")
    void createLike_userNotFound() {
        // Given
        Long userId = 999L;
        Long postId = 1L;

        LikeCreateRequest request =
                new LikeCreateRequest(postId);

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> likeService.createLike(
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

        verify(likeRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글로 좋아요 생성 시 예외가 발생한다")
    void createLike_postNotFound() {
        // Given
        Long userId = 1L;
        Long postId = 999L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );
        ReflectionTestUtils.setField(user, "id", userId);

        LikeCreateRequest request =
                new LikeCreateRequest(postId);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.findById(postId))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> likeService.createLike(
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

        verify(likeRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("좋아요 전체 조회에 성공한다")
    void getLikes_success() {
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

        Like like1 = Like.create(user, post);
        ReflectionTestUtils.setField(like1, "id", 1L);

        Like like2 = Like.create(user, post);
        ReflectionTestUtils.setField(like2, "id", 2L);

        given(likeRepository.findAll())
                .willReturn(List.of(like1, like2));

        // When
        List<LikeResponse> responses =
                likeService.getLikes();

        // Then
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).likeId());
        assertEquals(2L, responses.get(1).likeId());

        verify(likeRepository, times(1))
                .findAll();
    }

    @Test
    @DisplayName("사용자와 게시글 기준으로 좋아요 단건 조회에 성공한다")
    void getLikeByUserAndPost_success() {
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

        Like like = Like.create(user, post);
        ReflectionTestUtils.setField(like, "id", 1L);

        given(likeRepository.findByUserIdAndPostId(userId, postId))
                .willReturn(Optional.of(like));

        // When
        LikeResponse response =
                likeService.getLikeByUserAndPost(
                        userId,
                        postId
                );

        // Then
        assertEquals(1L, response.likeId());
        assertEquals(userId, response.userId());
        assertEquals(postId, response.postId());

        verify(likeRepository, times(1))
                .findByUserIdAndPostId(userId, postId);
    }

    @Test
    @DisplayName("특정 게시글 좋아요 목록 조회에 성공한다")
    void getLikesByPost_success() {
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

        Like like = Like.create(user, post);
        ReflectionTestUtils.setField(like, "id", 1L);

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(likeRepository.findByPostId(postId))
                .willReturn(List.of(like));

        // When
        List<LikeResponse> responses =
                likeService.getLikesByPost(postId);

        // Then
        assertEquals(1, responses.size());
        assertEquals(
                postId,
                responses.get(0).postId()
        );

        verify(postRepository, times(1))
                .findById(postId);

        verify(likeRepository, times(1))
                .findByPostId(postId);
    }

    @Test
    @DisplayName("특정 게시글 좋아요 개수 조회에 성공한다")
    void getLikeCountByPost_success() {
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

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        given(likeRepository.countByPostId(postId))
                .willReturn(3L);

        // When
        LikeCountResponse response =
                likeService.getLikeCountByPost(postId);

        // Then
        assertEquals(postId, response.postId());
        assertEquals(3L, response.likeCount());

        verify(postRepository, times(1))
                .findById(postId);

        verify(likeRepository, times(1))
                .countByPostId(postId);
    }

    @Test
    @DisplayName("사용자와 게시글 기준으로 좋아요 삭제에 성공한다")
    void deleteLikeByUserAndPost_success() {
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

        Like like = Like.create(user, post);
        ReflectionTestUtils.setField(like, "id", 1L);

        given(likeRepository.findByUserIdAndPostId(userId, postId))
                .willReturn(Optional.of(like));

        // When
        likeService.deleteLikeByUserAndPost(
                userId,
                postId
        );

        // Then
        verify(likeRepository, times(1))
                .findByUserIdAndPostId(userId, postId);

        verify(likeRepository, times(1))
                .delete(like);
    }
}