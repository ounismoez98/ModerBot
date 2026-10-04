import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PostForm } from '../../components/post-form/post-form';
import { PostList } from '../../components/post-list/post-list';
import { AuthService } from '../../services/auth.service';
import { PostService } from '../../services/post.service';
import { Post, PostRequest } from '../../models/post';

@Component({
  selector: 'app-student-home',
  imports: [PostForm, PostList, RouterLink],
  templateUrl: './student-home.html',
  styleUrls: ['../../app.css', './student-home.css'],
})
export class StudentHome implements OnInit {
  @ViewChild(PostForm) private postForm?: PostForm;

  readonly posts = signal<Post[]>([]);
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
    this.error.set('');
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

  submitPost(request: PostRequest): void {
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    this.postService.createPost(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.postForm?.reset();
        this.message.set('Votre commentaire est envoyé au modérateur. Il apparaîtra après validation.');
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.saving.set(false);
      },
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
        ? 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.'
        : `La demande a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
