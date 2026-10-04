import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginPage {
  readonly username = signal('');
  readonly password = signal('');
  readonly loading = signal(false);
  readonly error = signal('');

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  async submit(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    const username = this.username().trim();
    const password = this.password();

    try {
      const response = await this.authService.login(username, password);
      if (response.role !== 'STUDENT' && response.role !== 'MODERATOR') {
        this.error.set('Ce compte ne possède pas de rôle autorisé.');
        this.loading.set(false);
        return;
      }
      this.authService.acceptLogin(response.username, response.role);
      this.loading.set(false);
      void this.router.navigateByUrl(response.role === 'STUDENT' ? '/home' : '/moderation');
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      this.loading.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) {
        return 'Identifiant ou mot de passe incorrect.';
      }
      return error.status === 0
        ? 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.'
        : `La connexion a échoué (HTTP ${error.status}).`;
    }
    return 'Une erreur inattendue est survenue pendant la connexion.';
  }
}
