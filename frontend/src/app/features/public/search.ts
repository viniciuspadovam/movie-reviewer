import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';

import { errorMessage } from '../../core/errors';
import { kindAndYear, starsLabel } from '../../core/format';
import { MediaType, SearchFilters, SearchSort } from '../../core/models';
import { PublicApi } from '../../core/public-api';
import { Seo } from '../../core/seo';
import { Poster } from '../../shared/poster';
import { StarRating } from '../../shared/star-rating';

const RATING_OPTIONS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

function optionalNumber(value: string | undefined): number | undefined {
  const parsed = value ? Number(value) : NaN;
  return Number.isFinite(parsed) ? parsed : undefined;
}

@Component({
  selector: 'app-search',
  imports: [RouterLink, Poster, StarRating],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './search.html',
  styleUrl: './search.css',
})
export class Search {
  private readonly api = inject(PublicApi);
  private readonly router = inject(Router);
  protected readonly kindAndYear = kindAndYear;

  readonly q = input<string>();
  readonly type = input<string>();
  readonly genre = input<string>();
  readonly minRating = input<string>();
  readonly maxRating = input<string>();
  readonly year = input<string>();
  readonly sort = input<string>();
  readonly page = input<string>();

  protected readonly filters = computed<SearchFilters>(() => ({
    q: this.q()?.trim() || undefined,
    type:
      this.type() === 'MOVIE' || this.type() === 'SERIES' ? (this.type() as MediaType) : undefined,
    genre: optionalNumber(this.genre()),
    minRating: optionalNumber(this.minRating()),
    maxRating: optionalNumber(this.maxRating()),
    year: optionalNumber(this.year()),
    sort:
      (['RECENT', 'RATING', 'TITLE'] as const).find((value) => value === this.sort()) ?? 'RECENT',
    page: optionalNumber(this.page()) ?? 0,
  }));

  protected readonly results = rxResource({
    params: () => this.filters(),
    stream: ({ params }) => this.api.searchTitles(params),
  });

  protected readonly genres = rxResource({ stream: () => this.api.genres() });

  protected readonly errorText = computed(() =>
    this.results.error()
      ? errorMessage(this.results.error(), 'Não foi possível buscar agora.')
      : null,
  );

  protected readonly ratingOptions = RATING_OPTIONS;
  protected readonly starsLabel = starsLabel;

  constructor() {
    inject(Seo).set('Buscar');
  }

  protected submitQuery(event: Event, query: string): void {
    event.preventDefault();
    this.apply({ q: query.trim() || undefined });
  }

  protected apply(changes: Partial<SearchFilters>): void {
    const next: SearchFilters = { ...this.filters(), page: 0, ...changes };
    const queryParams = Object.fromEntries(
      Object.entries(next).map(([key, value]) => [key, this.isDefault(key, value) ? null : value]),
    );
    void this.router.navigate(['/search'], { queryParams, replaceUrl: changes.page === undefined });
  }

  protected clear(): void {
    void this.router.navigate(['/search']);
  }

  protected selectValue(event: Event): string | undefined {
    const value = (event.target as HTMLSelectElement | HTMLInputElement).value;
    return value === '' ? undefined : value;
  }

  protected numberValue(event: Event): number | undefined {
    return optionalNumber(this.selectValue(event));
  }

  protected sortValue(event: Event): SearchSort {
    return this.selectValue(event) as SearchSort;
  }

  protected typeValue(event: Event): MediaType | undefined {
    return this.selectValue(event) as MediaType | undefined;
  }

  private isDefault(key: string, value: unknown): boolean {
    return (
      value === undefined ||
      (key === 'page' && value === 0) ||
      (key === 'sort' && value === 'RECENT')
    );
  }
}
