package com.example.studentforum.service;

import com.example.studentforum.entity.Post;
import com.example.studentforum.entity.PostRequest;
import com.example.studentforum.entity.PostStatus;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post submit(PostRequest request, ModerationResponse moderation) {
        return postRepository.save(new Post(
                request.title(),
                request.content(),
                request.author(),
                moderation.scoreToxic(),
                moderation.decision().name(),
                moderation.sujet(),
                moderation.explication()
        ));
    }

    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<Post> findPending() {
        return postRepository.findByStatusIsNullOrStatusOrderByCreatedAtDesc(PostStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Post findById(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostNotFoundException(id);
        }
        return post;
    }

    public Post update(Long id, PostRequest request, ModerationResponse moderation) {
        Post post = findById(id);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setAuthor(request.author());
        post.setToxicityScore(moderation.scoreToxic());
        post.setSuggestedDecision(moderation.decision().name());
        post.setSuggestedSubject(moderation.sujet());
        post.setSuggestedExplanation(moderation.explication());
        post.clearModeratorDecision();
        post.setStatus(PostStatus.PENDING);
        return postRepository.save(post);
    }

    public Post decide(Long id, PostStatus status, String reason, String moderator) {
        if (status != PostStatus.PUBLISHED && status != PostStatus.REJECTED) {
            throw new IllegalArgumentException("Une décision de modération doit publier ou rejeter la publication.");
        }
        if (reason == null || reason.isBlank() || reason.length() > 1000) {
            throw new IllegalArgumentException("Un motif de modération de 1 à 1000 caractères est obligatoire.");
        }
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new IllegalStateException("Cette publication a déjà été traitée.");
        }
        post.setStatus(status);
        post.recordModeratorDecision(reason.trim(), moderator, java.time.LocalDateTime.now());
        return postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public ModerationStatistics statistics() {
        return new ModerationStatistics(
                postRepository.countByStatus(PostStatus.PENDING) + postRepository.countByStatusIsNull(),
                postRepository.countByStatus(PostStatus.PUBLISHED),
                postRepository.countByStatus(PostStatus.REJECTED)
        );
    }

    public void delete(Long id) {
        Post post = findById(id);
        postRepository.delete(post);
    }

    public record ModerationStatistics(long pending, long published, long rejected) {
    }
}
