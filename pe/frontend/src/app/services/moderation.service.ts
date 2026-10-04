import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CommentRequest, ModerationResponse } from '../models/moderation';

@Injectable({
  providedIn: 'root',
})
export class ModerationService {
  private readonly apiUrl = 'http://localhost:8080/api/moderation';

  constructor(private readonly http: HttpClient) {}

  analyze(text: string): Observable<ModerationResponse> {
    const request: CommentRequest = { text };
    return this.http.post<ModerationResponse>(`${this.apiUrl}/analyze`, request, {
      withCredentials: true,
    });
  }

  analyzeBatch(commentaires: string[]): Observable<ModerationResponse[]> {
    const request: CommentRequest = { commentaires };
    return this.http.post<ModerationResponse[]>(`${this.apiUrl}/batch`, request, {
      withCredentials: true,
    });
  }
}
