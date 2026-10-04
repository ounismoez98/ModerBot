import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModerationDecision, ModerationResponse } from '../../models/moderation';
import { ModerationService } from '../../services/moderation.service';

@Component({
  selector: 'app-moderation-batch',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './moderation-batch.html',
  styleUrl: './moderation-batch.css',
})
export class ModerationBatch {
  readonly input = signal('');
  readonly results = signal<ModerationResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly publishedCount = computed(
    () => this.results().filter((item) => item.decision === 'PUBLIER').length,
  );
  readonly reviewCount = computed(
    () => this.results().filter((item) => item.decision === 'A_VERIFIER').length,
  );
  readonly blockedCount = computed(
    () => this.results().filter((item) => item.decision === 'BLOQUER').length,
  );
  readonly inputCount = computed(
    () => this.input().split(/\r?\n/).filter((line) => line.trim()).length,
  );

  constructor(private readonly moderationService: ModerationService) {}

  analyzeBatch(): void {
    const comments = this.input()
      .split(/\r?\n/)
      .map((comment) => comment.trim())
      .filter(Boolean);

    if (comments.length === 0) {
      this.error.set('Ajoutez au moins un commentaire, un par ligne.');
      this.results.set([]);
      return;
    }
    if (comments.length > 100) {
      this.error.set('La file est limitée à 100 commentaires par analyse.');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.results.set([]);
    this.moderationService.analyzeBatch(comments).subscribe({
      next: (results) => {
        this.results.set(results);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  decisionLabel(decision: ModerationDecision): string {
    switch (decision) {
      case 'PUBLIER':
        return 'Avis favorable';
      case 'A_VERIFIER':
        return 'Avis : à vérifier';
      case 'BLOQUER':
        return 'Avis défavorable';
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const apiMessage = error.error?.message;
      if (typeof apiMessage === 'string') {
        return apiMessage;
      }
      const moderationMessage = error.error?.erreur;
      if (typeof moderationMessage === 'string') {
        return moderationMessage;
      }
      return error.status === 0
        ? 'Impossible de joindre le backend. Vérifiez qu’il est démarré.'
        : `L’analyse par lot a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue pendant le traitement de la file.';
  }
}
