import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { errorMessage, isNotFound } from '../../core/errors';
import {
  backdropUrl,
  formatDate,
  formatDateTime,
  mediaTypeLabel,
  ratingDeltaLabel,
  sessionLabel,
} from '../../core/format';
import { Title } from '../../core/models';
import { PublicApi } from '../../core/public-api';
import { Seo } from '../../core/seo';
import { Poster } from '../../shared/poster';
import { SpoilerGuard } from '../../shared/spoiler-guard';
import { StarRating } from '../../shared/star-rating';
import { NotFound } from './not-found';

@Component({
  selector: 'app-title-detail',
  imports: [RouterLink, Poster, StarRating, SpoilerGuard, NotFound],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './title-detail.html',
  styleUrl: './title-detail.css',
})
export class TitleDetail {
  private readonly api = inject(PublicApi);
  private readonly seo = inject(Seo);

  readonly slug = input.required<string>();

  protected readonly page = rxResource({
    params: () => this.slug(),
    stream: ({ params }) => this.api.titlePage(params),
  });

  protected readonly notFound = computed(() => isNotFound(this.page.error()));
  protected readonly errorText = computed(() =>
    this.page.error() ? errorMessage(this.page.error(), 'Não foi possível carregar a obra.') : null,
  );
  protected readonly backdrop = computed(() => {
    const url = this.page.hasValue() ? backdropUrl(this.page.value().title.backdropPath) : null;
    return url ? `url('${url}')` : null;
  });

  protected readonly formatDate = formatDate;
  protected readonly formatDateTime = formatDateTime;
  protected readonly ratingDeltaLabel = ratingDeltaLabel;
  protected readonly sessionLabel = sessionLabel;

  constructor() {
    effect(() => {
      if (this.page.hasValue()) {
        const value = this.page.value();
        this.seo.set(value.title.name, value.title.overview);
      }
    });
  }

  protected details(title: Title): string {
    const parts = [mediaTypeLabel(title.mediaType)];
    if (title.releaseYear) {
      parts[0] += ` de ${title.releaseYear}`;
    }
    if (title.runtimeMinutes) {
      parts.push(`${title.runtimeMinutes} min`);
    }
    if (title.seasonCount) {
      parts.push(title.seasonCount === 1 ? '1 temporada' : `${title.seasonCount} temporadas`);
    }
    return parts.join(', ');
  }
}
