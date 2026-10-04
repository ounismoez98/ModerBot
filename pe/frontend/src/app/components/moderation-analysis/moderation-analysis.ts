import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModerationDecision, ModerationResponse } from '../../models/moderation';
import { ModerationService } from '../../services/moderation.service';

@Component({
  selector: 'app-moderation-analysis',
  imports: [FormsModule, DecimalPipe, KeyValuePipe],
  templateUrl: './moderation-analysis.html',
  styleUrl: './moderation-analysis.css',
})
export class ModerationAnalysis {
  readonly text = signal('');
  readonly result = signal<ModerationResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  constructor(private readonly moderationService: ModerationService) {}

  analyze(): void {
    const text = this.text().trim();
    if (!text) {
      this.error.set('Saisissez un commentaire avant de lancer l’analyse.');
      this.result.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    this.moderationService.analyze(text).subscribe({
      next: (result) => {
        this.result.set(result);
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
        : `L’analyse a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue pendant l’analyse.';
  }
}
