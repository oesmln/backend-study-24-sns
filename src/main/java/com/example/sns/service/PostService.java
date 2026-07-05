package com.example.sns.service;

import com.example.sns.dto.PostCreateRequest;
import com.example.sns.dto.PostResponse;
import com.example.sns.dto.PostUpdateRequest;
import com.example.sns.entity.Post;
import com.example.sns.entity.User;
import com.example.sns.exception.CustomException;
import com.example.sns.exception.ErrorCode;
import com.example.sns.repository.CommentRepository;
import com.example.sns.repository.LikeRepository;
import com.example.sns.repository.PostRepository;
import com.example.sns.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            LikeRepository likeRepository,
            CommentRepository commentRepository
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    // 게시글 작성
    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        Post post = Post.create(
                request.title(),
                request.content(),
                user
        );

        Post savedPost = postRepository.save(post);

        return PostResponse.from(savedPost);
    }

    // 게시글 전체 조회
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostResponse::from);
    }

    // 게시글 조회
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.POST_NOT_FOUND)
                );

        return PostResponse.from(post);
    }

    // 게시글 수정
    @Transactional
    public PostResponse updatePost(
            Long postId,
            PostUpdateRequest request
    ) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.POST_NOT_FOUND)
                );

        post.update(
                request.title(),
                request.content()
        );

        return PostResponse.from(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.POST_NOT_FOUND)
                );

        // 게시글을 참조하는 데이터부터 삭제
        likeRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);

        // 마지막에 게시글 삭제
        postRepository.delete(post);
    }
}