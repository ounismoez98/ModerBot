import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ModerationAnalysis } from '../../components/moderation-analysis/moderation-analysis';
import { ModerationBatch } from '../../components/moderation-batch/moderation-batch';
import { ModeratorQueue } from '../../components/moderator-queue/moderator-queue';
import { PostForm } from '../../components/post-form/post-form';
import { PostList } from '../../components/post-list/post-list';
import { Post, PostRequest } from '../../models/post';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-moderator-dashboard',
  imports: [ModerationAnalysis, ModerationBatch, ModeratorQueue, PostForm, PostList, RouterLink],
  templateUrl: './moderator-dashboard.html',
  styleUrls: ['../../app.css', './moderator-dashboard.css'],
})
export class ModeratorDashboard implements OnInit {
  readonly posts = signal<Post[]>([]);
  readonly editingPost = signal<Post | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');

  constructor(
    readonly authService: AuthService,
    private readonly postService: PostService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.loading.set(true);
    this.postService.getPosts().subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  savePost(request: PostRequest): void {
    const post = this.editingPost();
    this.saving.set(true);
    this.error.set('');
    const result = post
      ? this.postService.updatePost(post.id, request)
      : this.postService.createPost(request);
    result.subscribe({
      next: () => {
        this.saving.set(false);
        this.editingPost.set(null);
        this.message.set('La soumission a été envoyée dans la file de modération.');
        this.loadPosts();
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.saving.set(false);
      },
    });
  }

  editPost(post: Post): void {
    this.editingPost.set(post);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingPost.set(null);
  }

  deletePost(post: Post): void {
    if (!window.confirm(`Supprimer la publication « ${post.title} » ?`)) {
      return;
    }
    this.postService.deletePost(post.id).subscribe({
      next: () => {
        this.message.set('Publication supprimée.');
        this.loadPosts();
      },
      error: (error: unknown) => this.error.set(this.errorMessage(error)),
    });
  }

  async logout(): Promise<void> {
    try {
      await this.authService.logout();
      await this.router.navigateByUrl('/login');
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
        await this.router.navigateByUrl('/login');
        return;
      }
      this.error.set(this.errorMessage(error));
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const message = error.error?.message;
      if (typeof message === 'string') {
        return message;
      }
      return error.status === 0
        ? 'Impossible de joindre le backend. Vérifiez qu’il est démarré.'
        : `La demande a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
