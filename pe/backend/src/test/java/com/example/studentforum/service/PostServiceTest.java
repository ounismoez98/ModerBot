package com.example.studentforum.service;

import com.example.studentforum.entity.Post;
import com.example.studentforum.entity.PostRequest;
import com.example.studentforum.entity.PostStatus;
import com.example.studentforum.moderation.Decision;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void submitSavesPostAsPendingRegardlessOfAiSuggestion() {
        PostRequest request = new PostRequest("Révisions", "Groupe de révision jeudi.", "Alice");
        ModerationResponse moderation = new ModerationResponse(
                request.content(), 0.91, 0.09, "cours", Map.of(), Decision.BLOQUER, null,
                "Avis IA défavorable.");
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post post = postService.submit(request, moderation);

        assertEquals(request.title(), post.getTitle());
        assertEquals(request.content(), post.getContent());
        assertEquals(request.author(), post.getAuthor());
        assertEquals(PostStatus.PENDING, post.getStatus());
        assertEquals(0.91, post.getToxicityScore());
        assertEquals("BLOQUER", post.getSuggestedDecision());
        assertEquals("Avis IA défavorable.", post.getSuggestedExplanation());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void moderatorCanPublishOnlyPendingPosts() {
        Post pending = new Post("Title", "Content", "Alice");
        when(postRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(postRepository.save(pending)).thenReturn(pending);

        Post published = postService.decide(1L, PostStatus.PUBLISHED, "Contenu pertinent.", "moderator");

        assertEquals(PostStatus.PUBLISHED, published.getStatus());
        assertEquals("Contenu pertinent.", published.getModerationReason());
        assertEquals("moderator", published.getModeratedBy());
        assertNotNull(published.getModeratedAt());
        verify(postRepository).save(pending);
        assertThrows(IllegalStateException.class,
                () -> postService.decide(1L, PostStatus.REJECTED, "Motif", "moderator"));
    }

    @Test
    void publicFeedContainsOnlyExplicitlyPublishedPosts() {
        when(postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)).thenReturn(List.of());

        postService.findAll();

        verify(postRepository).findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
    }

    @Test
    void moderationQueueIncludesLegacyPostsWithoutAStatus() {
        when(postRepository.findByStatusIsNullOrStatusOrderByCreatedAtDesc(PostStatus.PENDING))
                .thenReturn(List.of());

        postService.findPending();

        verify(postRepository).findByStatusIsNullOrStatusOrderByCreatedAtDesc(PostStatus.PENDING);
    }

    @Test
    void statisticsIncludeLegacyPostsWithoutAStatus() {
        when(postRepository.countByStatus(PostStatus.PENDING)).thenReturn(2L);
        when(postRepository.countByStatusIsNull()).thenReturn(1L);
        when(postRepository.countByStatus(PostStatus.PUBLISHED)).thenReturn(5L);
        when(postRepository.countByStatus(PostStatus.REJECTED)).thenReturn(3L);

        PostService.ModerationStatistics statistics = postService.statistics();

        assertEquals(3L, statistics.pending());
        assertEquals(5L, statistics.published());
        assertEquals(3L, statistics.rejected());
    }

    @Test
    void moderatorDecisionRequiresAnExplanation() {
        assertThrows(IllegalArgumentException.class,
                () -> postService.decide(1L, PostStatus.PUBLISHED, "  ", "moderator"));
    }

    @Test
    void findByIdThrowsWhenPostDoesNotExist() {
        when(postRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.findById(42L));
    }

    @Test
    void updateChangesPostFields() {
        Post existing = new Post("Ancien titre", "Ancien texte", "Alice");
        existing.setStatus(PostStatus.PUBLISHED);
        PostRequest request = new PostRequest("Nouveau titre", "Nouveau texte", "Bob");
        ModerationResponse moderation = new ModerationResponse(
                request.content(), 0.08, 0.92, "cours", Map.of(), Decision.PUBLIER, null, "Avis favorable");
        when(postRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(postRepository.save(existing)).thenReturn(existing);

        Post updated = postService.update(1L, request, moderation);

        assertEquals("Nouveau titre", updated.getTitle());
        assertEquals("Nouveau texte", updated.getContent());
        assertEquals("Bob", updated.getAuthor());
        assertEquals(PostStatus.PENDING, updated.getStatus());
        assertEquals(0.08, updated.getToxicityScore());
        verify(postRepository).save(existing);
    }
}
