package com.example.studentforum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10_000)
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", length = 20)
    private PostStatus status = PostStatus.PENDING;

    private Double toxicityScore;

    @Column(length = 30)
    private String suggestedDecision;

    @Column(length = 100)
    private String suggestedSubject;

    @Column(length = 500)
    private String suggestedExplanation;

    @Column(length = 1000)
    private String moderationReason;

    @Column(length = 100)
    private String moderatedBy;

    private LocalDateTime moderatedAt;

    protected Post() {
    }

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Post(String title, String content, String author, Double toxicityScore,
                String suggestedDecision, String suggestedSubject, String suggestedExplanation) {
        this(title, content, author);
        this.toxicityScore = toxicityScore;
        this.suggestedDecision = suggestedDecision;
        this.suggestedSubject = suggestedSubject;
        this.suggestedExplanation = suggestedExplanation;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PostStatus getStatus() {
        return status == null ? PostStatus.PENDING : status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public Double getToxicityScore() {
        return toxicityScore;
    }

    public void setToxicityScore(Double toxicityScore) {
        this.toxicityScore = toxicityScore;
    }

    public String getSuggestedDecision() {
        return suggestedDecision;
    }

    public void setSuggestedDecision(String suggestedDecision) {
        this.suggestedDecision = suggestedDecision;
    }

    public String getSuggestedSubject() {
        return suggestedSubject;
    }

    public void setSuggestedSubject(String suggestedSubject) {
        this.suggestedSubject = suggestedSubject;
    }

    public String getSuggestedExplanation() {
        return suggestedExplanation;
    }

    public void setSuggestedExplanation(String suggestedExplanation) {
        this.suggestedExplanation = suggestedExplanation;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public String getModeratedBy() {
        return moderatedBy;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void recordModeratorDecision(String reason, String moderator, LocalDateTime decidedAt) {
        this.moderationReason = reason;
        this.moderatedBy = moderator;
        this.moderatedAt = decidedAt;
    }

    public void clearModeratorDecision() {
        moderationReason = null;
        moderatedBy = null;
        moderatedAt = null;
    }
}
