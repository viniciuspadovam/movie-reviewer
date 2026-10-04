import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  numberAttribute,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AdminApi } from '../../core/admin-api';
import { errorMessage, fieldErrors } from '../../core/errors';
import { todayIso, nameWithYear } from '../../core/format';
import { ReviewStatus, TitleSummary } from '../../core/models';
import { Seo } from '../../core/seo';
import { ConfirmDialog } from '../../shared/confirm-dialog';
import { Markdown } from '../../shared/markdown';
import { Poster } from '../../shared/poster';
import { StarInput } from '../../shared/star-input';

@Component({
  selector: 'app-review-editor',
  imports: [RouterLink, Poster, StarInput, Markdown, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-editor.html',
  styleUrls: ['./admin.css', './review-editor.css'],
})
export class ReviewEditor implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  protected readonly nameWithYear = nameWithYear;
  private readonly seo = inject(Seo);
  private readonly confirm = viewChild.required(ConfirmDialog);

  readonly id = input(undefined, { transform: numberAttribute });
  readonly titleId = input(undefined, { transform: numberAttribute });

  protected readonly title = signal<TitleSummary | null>(null);
  protected readonly savedStatus = signal<ReviewStatus | null>(null);
  protected readonly watchedOn = signal(todayIso());
  protected readonly rating = signal<number | null>(null);
  protected readonly content = signal('');
  protected readonly hasSpoilers = signal(false);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errors = signal<Record<string, string>>({});

  protected readonly isEditing = computed(() => Number.isFinite(this.id()));
  protected readonly isPublished = computed(() => this.savedStatus() === 'PUBLISHED');
  protected readonly today = todayIso();

  async ngOnInit(): Promise<void> {
    try {
      if (this.isEditing()) {
        const review = await firstValueFrom(this.api.review(this.id()!));
        this.title.set(review.title);
        this.savedStatus.set(review.status);
        this.watchedOn.set(review.watchedOn);
        this.rating.set(review.rating);
        this.content.set(review.content);
        this.hasSpoilers.set(review.hasSpoilers);
      } else {
        this.title.set(await firstValueFrom(this.api.title(this.titleId()!)));
      }
      this.seo.set(`${this.isEditing() ? 'Editar' : 'Nova'} review de ${this.title()!.name}`);
    } catch (error) {
      this.error.set(errorMessage(error, 'Não foi possível abrir o editor.'));
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(event: Event, status: ReviewStatus): Promise<void> {
    event.preventDefault();
    const rating = this.rating();
    const localErrors: Record<string, string> = {};
    if (!rating) {
      localErrors['rating'] = 'Escolha a nota.';
    }
    if (status === 'PUBLISHED' && !this.content().trim()) {
      localErrors['content'] = 'Escreva o texto antes de publicar.';
    }
    this.errors.set(localErrors);
    if (Object.keys(localErrors).length) {
      return;
    }
    const request = {
      watchedOn: this.watchedOn(),
      rating: rating!,
      content: this.content(),
      hasSpoilers: this.hasSpoilers(),
      status,
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      const saved = this.isEditing()
        ? await firstValueFrom(this.api.updateReview(this.id()!, request))
        : await firstValueFrom(this.api.createReview(this.titleId()!, request));
      await (saved.status === 'PUBLISHED'
        ? this.router.navigate(['/review', saved.id])
        : this.router.navigate(['/admin'], { queryParams: { status: 'DRAFT' } }));
    } catch (error) {
      this.errors.set(fieldErrors(error));
      this.error.set(errorMessage(error, 'Não foi possível salvar a review.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected async deleteTitle(): Promise<void> {
    const title = this.title();
    if (!title) {
      return;
    }
    const confirmed = await this.confirm().ask({
      title: `Excluir ${title.name}?`,
      message: 'A obra e todas as reviews dela, publicadas ou não, serão apagadas de vez.',
      confirmLabel: 'Excluir obra e reviews',
    });
    if (!confirmed) {
      return;
    }
    try {
      await firstValueFrom(this.api.deleteTitle(title.id, true));
      await this.router.navigate(['/admin']);
    } catch (error) {
      this.error.set(errorMessage(error, 'Não foi possível excluir a obra.'));
    }
  }

  protected inputValue(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected checked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }
}
