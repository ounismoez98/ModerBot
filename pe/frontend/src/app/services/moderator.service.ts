import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Post } from '../models/post';

export interface ModerationStatistics {
  pending: number;
  published: number;
  rejected: number;
}

@Injectable({ providedIn: 'root' })
export class ModeratorService {
  private readonly apiUrl = 'http://localhost:8080/api/moderation/queue';

  constructor(private readonly http: HttpClient) {}

  getPending(): Observable<Post[]> {
    return this.http.get<Post[]>(this.apiUrl, { withCredentials: true });
  }

  getStatistics(): Observable<ModerationStatistics> {
    return this.http.get<ModerationStatistics>(`${this.apiUrl}/statistics`, { withCredentials: true });
  }

  publish(id: number, reason: string): Observable<Post> {
    return this.http.post<Post>(`${this.apiUrl}/${id}/publish`, { reason }, { withCredentials: true });
  }

  reject(id: number, reason: string): Observable<Post> {
    return this.http.post<Post>(`${this.apiUrl}/${id}/reject`, { reason }, { withCredentials: true });
  }
}
