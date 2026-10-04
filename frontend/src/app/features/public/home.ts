import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { Auth } from '../../core/auth';
import { errorMessage } from '../../core/errors';
import { formatDate, kindAndYear } from '../../core/format';
import { ReviewSummary } from '../../core/models';
import { PublicApi } from '../../core/public-api';
import { Seo } from '../../core/seo';
import { Poster } from '../../shared/poster';
import { SpoilerGuard } from '../../shared/spoiler-guard';
import { StarRating } from '../../shared/star-rating';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-home',
  imports: [RouterLink, Poster, StarRating, SpoilerGuard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly api = inject(PublicApi);
  protected readonly auth = inject(Auth);

  protected readonly reviews = signal<ReviewSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  private readonly nextPage = signal(0);
  private readonly totalPages = signal(0);

  protected readonly lead = computed(() => this.reviews()[0] ?? null);
  protected readonly others = computed(() => this.reviews().slice(1));
  protected readonly hasMore = computed(() => this.nextPage() < this.totalPages());

  protected readonly formatDate = formatDate;
  protected readonly kindAndYear = kindAndYear;

  constructor() {
    inject(Seo).set(null);
  }

  ngOnInit(): void {
    this.loadMore();
  }

  protected loadMore(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.latestReviews(this.nextPage(), PAGE_SIZE).subscribe({
      next: (page) => {
        this.reviews.update((current) => [...current, ...page.items]);
        this.nextPage.set(page.page + 1);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(errorMessage(error, 'Não foi possível carregar as reviews.'));
        this.loading.set(false);
      },
    });
  }
}
