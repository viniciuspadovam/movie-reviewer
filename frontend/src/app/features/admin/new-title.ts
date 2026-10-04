import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/admin-api';
import { errorMessage } from '../../core/errors';
import { MediaType, TmdbSearchResult } from '../../core/models';
import { Seo } from '../../core/seo';
import { Poster } from '../../shared/poster';

@Component({
  selector: 'app-new-title',
  imports: [Poster],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './new-title.html',
  styleUrls: ['./admin.css', './new-title.css'],
})
export class NewTitle {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);

  protected readonly mediaType = signal<MediaType>('MOVIE');
  protected readonly results = signal<TmdbSearchResult[] | null>(null);
  protected readonly searching = signal(false);
  protected readonly importingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    inject(Seo).set('Nova review');
  }

  protected async search(event: Event, query: string): Promise<void> {
    event.preventDefault();
    if (!query.trim()) {
      this.error.set('Digite o nome do filme ou da série.');
      return;
    }
    this.searching.set(true);
    this.error.set(null);
    try {
      this.results.set(await firstValueFrom(this.api.searchTmdb(query.trim(), this.mediaType())));
    } catch (error) {
      this.error.set(errorMessage(error, 'Não foi possível buscar no TMDB.'));
    } finally {
      this.searching.set(false);
    }
  }

  protected async choose(result: TmdbSearchResult): Promise<void> {
    this.importingId.set(result.tmdbId);
    this.error.set(null);
    try {
      const title = await firstValueFrom(this.api.importTitle(result.tmdbId, result.mediaType));
      await this.router.navigate(['/admin/titles', title.id, 'reviews', 'new']);
    } catch (error) {
      this.error.set(errorMessage(error, 'Não foi possível cadastrar a obra.'));
      this.importingId.set(null);
    }
  }
}
