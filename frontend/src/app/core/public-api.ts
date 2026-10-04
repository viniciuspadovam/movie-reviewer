import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { apiUrl } from './api-url';
import {
  Genre,
  Page,
  ReviewDetail,
  ReviewSummary,
  SearchFilters,
  TitlePage,
  TitleSearchItem,
} from './models';

@Injectable({ providedIn: 'root' })
export class PublicApi {
  private readonly http = inject(HttpClient);

  latestReviews(page = 0, size = 10): Observable<Page<ReviewSummary>> {
    return this.http.get<Page<ReviewSummary>>(apiUrl('/reviews/latest'), { params: { page, size } });
  }

  review(id: number): Observable<ReviewDetail> {
    return this.http.get<ReviewDetail>(apiUrl(`/reviews/${id}`));
  }

  titlePage(slug: string): Observable<TitlePage> {
    return this.http.get<TitlePage>(apiUrl(`/titles/${encodeURIComponent(slug)}`));
  }

  searchTitles(filters: SearchFilters): Observable<Page<TitleSearchItem>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<TitleSearchItem>>(apiUrl('/titles'), { params });
  }

  genres(): Observable<Genre[]> {
    return this.http.get<Genre[]>(apiUrl('/genres'));
  }
}
