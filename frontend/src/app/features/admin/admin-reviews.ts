import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/admin-api';
import { errorMessage } from '../../core/errors';
import { formatDate, formatDateTime } from '../../core/format';
import { AdminReview, ReviewStatus } from '../../core/models';
import { Seo } from '../../core/seo';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { StarRating } from '../../shared/star-rating';

const FILTERS: { label: string; status: ReviewStatus | null }[] = [
  { label: 'Todas', status: null },
  { label: 'Rascunhos', status: 'DRAFT' },
  { label: 'Publicadas', status: 'PUBLISHED' },
];

@Component({
  selector: 'app-admin-reviews',
  imports: [RouterLink, StarRating, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-reviews.html',
  styleUrl: './admin.css',
})
export class AdminReviews {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  private readonly confirm = viewChild.required(ConfirmDialog);

  readonly status = input<string>();
  readonly page = input<string>();

  protected readonly filters = FILTERS;
  protected readonly currentStatus = computed<ReviewStatus | null>(() =>
    this.status() === 'DRAFT' || this.status() === 'PUBLISHED'
      ? (this.status() as ReviewStatus)
      : null,
  );
  private readonly currentPage = computed(() => Math.max(0, Number(this.page() ?? 0) || 0));

  protected readonly reviews = rxResource({
    params: () => ({ status: this.currentStatus(), page: this.currentPage() }),
    stream: ({ params }) => this.api.reviews(params.status, params.page),
  });

  protected readonly errorText = computed(() =>
    this.reviews.error()
      ? errorMessage(this.reviews.error(), 'Não foi possível carregar as reviews.')
      : null,
  );

  protected readonly formatDate = formatDate;
  protected readonly formatDateTime = formatDateTime;
  protected readonly actionError = signal<string | null>(null);

  constructor() {
    inject(Seo).set('Painel');
  }

  protected goToPage(page: number): void {
    void this.router.navigate(['/admin'], {
      queryParams: { status: this.currentStatus(), page: page || null },
      queryParamsHandling: 'merge',
    });
  }

  protected async remove(review: AdminReview): Promise<void> {
    const confirmed = await this.confirm().ask({
      title: 'Excluir esta review?',
      message: `A review de ${review.title.name} assistida em ${formatDate(review.watchedOn)} será apagada de vez.`,
      confirmLabel: 'Excluir review',
    });
    if (!confirmed) {
      return;
    }
    try {
      await firstValueFrom(this.api.deleteReview(review.id));
      this.actionError.set(null);
      this.reviews.reload();
    } catch (error) {
      this.actionError.set(errorMessage(error, 'Não foi possível excluir a review.'));
    }
  }
}
