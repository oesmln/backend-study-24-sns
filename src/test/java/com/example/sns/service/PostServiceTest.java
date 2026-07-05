package com.example.sns.service;

import com.example.sns.dto.PostCreateRequest;
import com.example.sns.dto.PostResponse;
import com.example.sns.dto.PostUpdateRequest;
import com.example.sns.entity.Post;
import com.example.sns.entity.User;
import com.example.sns.exception.CustomException;
import com.example.sns.repository.CommentRepository;
import com.example.sns.repository.LikeRepository;
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
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 작성에 성공한다")
    void createPost_success() {
        // Given
        Long userId = 1L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                userId
        );

        PostCreateRequest request = new PostCreateRequest(
                "첫 번째 게시글",
                "게시글 내용입니다."
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        given(postRepository.save(any(Post.class)))
                .willAnswer(invocation -> {
                    Post post = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            post,
                            "id",
                            1L
                    );

                    return post;
                });

        // When
        PostResponse response =
                postService.createPost(userId, request);

        // Then
        assertAll(
                () -> assertEquals(
                        1L,
                        response.postId()
                ),
                () -> assertEquals(
                        "첫 번째 게시글",
                        response.title()
                ),
                () -> assertEquals(
                        "게시글 내용입니다.",
                        response.content()
                ),
                () -> assertEquals(
                        userId,
                        response.writerId()
                ),
                () -> assertEquals(
                        "테스트유저",
                        response.writerNickname()
                )
        );

        verify(userRepository, times(1))
                .findById(userId);

        verify(postRepository, times(1))
                .save(any(Post.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 게시글 작성 시 예외가 발생한다")
    void createPost_userNotFound() {
        // Given
        Long userId = 999L;

        PostCreateRequest request = new PostCreateRequest(
                "제목",
                "내용"
        );

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> postService.createPost(
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
                .save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 전체 조회에 성공한다")
    void getPosts_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        Post post1 = Post.create(
                "첫 번째 게시글",
                "내용1",
                user
        );

        ReflectionTestUtils.setField(
                post1,
                "id",
                1L
        );

        Post post2 = Post.create(
                "두 번째 게시글",
                "내용2",
                user
        );

        ReflectionTestUtils.setField(
                post2,
                "id",
                2L
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> postPage = new PageImpl<>(
                List.of(post1, post2),
                pageable,
                2
        );

        given(postRepository.findAll(pageable))
                .willReturn(postPage);

        // When
        Page<PostResponse> responses =
                postService.getPosts(pageable);

        // Then
        assertEquals(
                2,
                responses.getContent().size()
        );

        assertEquals(
                "첫 번째 게시글",
                responses.getContent().get(0).title()
        );

        assertEquals(
                "두 번째 게시글",
                responses.getContent().get(1).title()
        );

        assertEquals(
                2,
                responses.getTotalElements()
        );

        assertEquals(
                1,
                responses.getTotalPages()
        );

        verify(postRepository, times(1))
                .findAll(pageable);
    }

    @Test
    @DisplayName("게시글 단건 조회에 성공한다")
    void getPost_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        Post post = Post.create(
                "게시글 제목",
                "게시글 내용",
                user
        );

        ReflectionTestUtils.setField(
                post,
                "id",
                1L
        );

        given(postRepository.findById(1L))
                .willReturn(Optional.of(post));

        // When
        PostResponse response =
                postService.getPost(1L);

        // Then
        assertAll(
                () -> assertEquals(
                        1L,
                        response.postId()
                ),
                () -> assertEquals(
                        "게시글 제목",
                        response.title()
                ),
                () -> assertEquals(
                        "게시글 내용",
                        response.content()
                )
        );

        verify(postRepository, times(1))
                .findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외가 발생한다")
    void getPost_postNotFound() {
        // Given
        given(postRepository.findById(999L))
                .willReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(
                CustomException.class,
                () -> postService.getPost(999L)
        );

        assertEquals(
                "존재하지 않는 게시글입니다.",
                exception.getMessage()
        );

        verify(postRepository, times(1))
                .findById(999L);
    }

    @Test
    @DisplayName("게시글 수정에 성공한다")
    void updatePost_success() {
        // Given
        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        Post post = Post.create(
                "기존 제목",
                "기존 내용",
                user
        );

        ReflectionTestUtils.setField(
                post,
                "id",
                1L
        );

        PostUpdateRequest request =
                new PostUpdateRequest(
                        "수정된 제목",
                        "수정된 내용"
                );

        given(postRepository.findById(1L))
                .willReturn(Optional.of(post));

        // When
        PostResponse response =
                postService.updatePost(
                        1L,
                        request
                );

        // Then
        assertEquals(
                "수정된 제목",
                response.title()
        );

        assertEquals(
                "수정된 내용",
                response.content()
        );

        verify(postRepository, times(1))
                .findById(1L);
    }

    @Test
    @DisplayName("게시글 삭제 시 좋아요와 댓글도 함께 삭제한다")
    void deletePost_success() {
        // Given
        Long postId = 1L;

        User user = User.create(
                "test@test.com",
                "테스트유저",
                "password123"
        );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        Post post = Post.create(
                "삭제할 게시글",
                "내용",
                user
        );

        ReflectionTestUtils.setField(
                post,
                "id",
                postId
        );

        given(postRepository.findById(postId))
                .willReturn(Optional.of(post));

        // When
        postService.deletePost(postId);

        // Then
        verify(postRepository, times(1))
                .findById(postId);

        verify(likeRepository, times(1))
                .deleteAllByPostId(postId);

        verify(commentRepository, times(1))
                .deleteAllByPostId(postId);

        verify(postRepository, times(1))
                .delete(post);
    }
}