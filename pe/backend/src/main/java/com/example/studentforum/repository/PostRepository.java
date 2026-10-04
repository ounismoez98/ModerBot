package com.example.studentforum.repository;

import com.example.studentforum.entity.Post;
import com.example.studentforum.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);

    List<Post> findByStatusIsNullOrStatusOrderByCreatedAtDesc(PostStatus status);

    long countByStatus(PostStatus status);

    long countByStatusIsNull();
}
