import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { apiUrl } from './api-url';
import {
  AdminReview,
  MediaType,
  Page,
  ReviewRequest,
  ReviewStatus,
  Title,
  TmdbSearchResult,
} from './models';

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  reviews(status: ReviewStatus | null, page = 0, size = 20): Observable<Page<AdminReview>> {
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<Page<AdminReview>>(apiUrl('/admin/reviews'), { params });
  }

  review(id: number): Observable<AdminReview> {
    return this.http.get<AdminReview>(apiUrl(`/admin/reviews/${id}`));
  }

  createReview(titleId: number, request: ReviewRequest): Observable<AdminReview> {
    return this.http.post<AdminReview>(apiUrl(`/admin/titles/${titleId}/reviews`), request);
  }

  updateReview(id: number, request: ReviewRequest): Observable<AdminReview> {
    return this.http.put<AdminReview>(apiUrl(`/admin/reviews/${id}`), request);
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/admin/reviews/${id}`));
  }

  title(id: number): Observable<Title> {
    return this.http.get<Title>(apiUrl(`/admin/titles/${id}`));
  }

  searchTmdb(query: string, type: MediaType): Observable<TmdbSearchResult[]> {
    return this.http.get<TmdbSearchResult[]>(apiUrl('/admin/tmdb/search'), { params: { q: query, type } });
  }

  importTitle(tmdbId: number, mediaType: MediaType): Observable<Title> {
    return this.http.post<Title>(apiUrl('/admin/titles'), { tmdbId, mediaType });
  }

  deleteTitle(id: number, cascade: boolean): Observable<void> {
    return this.http.delete<void>(apiUrl(`/admin/titles/${id}`), { params: { cascade } });
  }
}
