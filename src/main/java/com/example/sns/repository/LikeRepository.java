package com.example.sns.repository;

import com.example.sns.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    List<Like> findByPostId(Long postId);

    Optional<Like> findByUserIdAndPostId(
            Long userId,
            Long postId
    );

    boolean existsByUserIdAndPostId(
            Long userId,
            Long postId
    );

    long countByPostId(Long postId);

    void deleteAllByPostId(Long postId);
}