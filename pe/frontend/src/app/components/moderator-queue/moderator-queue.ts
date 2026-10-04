import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Post } from '../../models/post';
import { ModerationStatistics, ModeratorService } from '../../services/moderator.service';

@Component({
  selector: 'app-moderator-queue',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './moderator-queue.html',
  styleUrl: './moderator-queue.css',
})
export class ModeratorQueue implements OnInit {
  @Output() published = new EventEmitter<void>();

  readonly posts = signal<Post[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly busyPostId = signal<number | null>(null);
  readonly reasons = signal<Record<number, string>>({});
  readonly statistics = signal<ModerationStatistics>({ pending: 0, published: 0, rejected: 0 });

  constructor(readonly moderatorService: ModeratorService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.moderatorService.getPending().subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.loading.set(false);
      },
    });
    this.moderatorService.getStatistics().subscribe({
      next: (statistics) => this.statistics.set(statistics),
      error: (error: unknown) => this.error.set(this.errorMessage(error)),
    });
  }

  setReason(postId: number, reason: string): void {
    this.reasons.update((current) => ({ ...current, [postId]: reason }));
  }

  decide(post: Post, publish: boolean): void {
    const reason = this.reasons()[post.id]?.trim() ?? '';
    if (!reason) {
      this.error.set('Saisissez un motif avant de prendre la décision.');
      return;
    }
    this.busyPostId.set(post.id);
    this.error.set('');
    const request = publish
      ? this.moderatorService.publish(post.id, reason)
      : this.moderatorService.reject(post.id, reason);
    request.subscribe({
      next: () => {
        this.message.set(publish ? 'Publication approuvée et ajoutée au forum.' : 'Publication rejetée.');
        this.reasons.update((current) => {
          const updated = { ...current };
          delete updated[post.id];
          return updated;
        });
        this.busyPostId.set(null);
        this.refresh();
        if (publish) {
          this.published.emit();
        }
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.busyPostId.set(null);
      },
    });
  }

  decisionLabel(decision: Post['suggestedDecision']): string {
    switch (decision) {
      case 'PUBLIER':
        return 'Avis favorable';
      case 'A_VERIFIER':
        return 'Avis : à vérifier';
      case 'BLOQUER':
        return 'Avis défavorable';
      default:
        return 'Avis indisponible';
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) {
        return 'Identifiants invalides ou accès modérateur refusé.';
      }
      const message = error.error?.message;
      if (typeof message === 'string') {
        return message;
      }
      return error.status === 0
        ? 'Impossible de joindre le backend.'
        : `La demande de modération a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
