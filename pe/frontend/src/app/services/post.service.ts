import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Post, PostRequest } from '../models/post';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private readonly apiUrl = 'http://localhost:8080/api/posts';

  constructor(private readonly http: HttpClient) {}

  getPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(this.apiUrl, { withCredentials: true });
  }

  createPost(request: PostRequest): Observable<Post> {
    return this.http.post<Post>(this.apiUrl, request, {
      withCredentials: true,
    });
  }

  updatePost(id: number, request: PostRequest): Observable<Post> {
    return this.http.put<Post>(`${this.apiUrl}/${id}`, request, {
      withCredentials: true,
    });
  }

  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      withCredentials: true,
    });
  }
}
