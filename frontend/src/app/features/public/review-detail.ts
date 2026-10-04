import { ChangeDetectionStrategy, Component, computed, effect, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { errorMessage, isNotFound } from '../../core/errors';
import { formatDate, formatDateTime, sessionLabel } from '../../core/format';
import { PublicApi } from '../../core/public-api';
import { Seo } from '../../core/seo';
import { Markdown } from '../../shared/markdown';
import { Poster } from '../../shared/poster';
import { SpoilerGuard } from '../../shared/spoiler-guard';
import { StarRating } from '../../shared/star-rating';
import { NotFound } from './not-found';

@Component({
  selector: 'app-review-detail',
  imports: [RouterLink, Poster, StarRating, SpoilerGuard, Markdown, NotFound],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-detail.html',
  styleUrl: './review-detail.css',
})
export class ReviewDetailPage {
  private readonly api = inject(PublicApi);
  private readonly seo = inject(Seo);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly review = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.api.review(params),
  });

  protected readonly notFound = computed(() => isNotFound(this.review.error()) || Number.isNaN(this.id()));
  protected readonly errorText = computed(() =>
    this.review.error() ? errorMessage(this.review.error(), 'Não foi possível carregar a review.') : null,
  );

  protected readonly formatDate = formatDate;
  protected readonly formatDateTime = formatDateTime;
  protected readonly sessionLabel = sessionLabel;

  constructor() {
    effect(() => {
      if (this.review.hasValue()) {
        const value = this.review.value();
        this.seo.set(
          `${value.title.name}, ${sessionLabel(value.sessionNumber)}`,
          value.hasSpoilers ? null : value.content.substring(0, 160),
        );
      }
    });
  }
}
