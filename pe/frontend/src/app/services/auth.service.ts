import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { LoginResponse, UserRole } from '../models/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private restorePromise: Promise<UserRole | null> | null = null;
  readonly role = signal<UserRole | null>(null);
  readonly username = signal('');

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Promise<LoginResponse> {
    return firstValueFrom(this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      { username, password },
      { withCredentials: true },
    ));
  }

  acceptLogin(username: string, role: UserRole): void {
    this.username.set(username);
    this.role.set(role);
    this.restorePromise = Promise.resolve(role);
  }

  async restoreSession(): Promise<UserRole | null> {
    if (this.restorePromise) {
      return this.restorePromise;
    }
    this.restorePromise = firstValueFrom(
      this.http.get<LoginResponse>(`${this.apiUrl}/me`, { withCredentials: true }),
    ).then((response) => {
      this.acceptLogin(response.username, response.role);
      return response.role;
    }).catch((error: unknown) => {
      this.clearLocalSession();
      if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
        return null;
      }
      this.restorePromise = null;
      throw error;
    });
    return this.restorePromise;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>(
        `${this.apiUrl}/logout`,
        {},
        { withCredentials: true },
      ));
    } finally {
      this.clearLocalSession();
    }
  }

  private clearLocalSession(): void {
    this.username.set('');
    this.role.set(null);
    this.restorePromise = null;
  }
}
