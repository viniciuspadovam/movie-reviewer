import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

import { PosterSize, posterUrl } from '../core/format';

@Component({
  selector: 'app-poster',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (src() && !failed()) {
      <img [src]="src()" [alt]="'Pôster de ' + name()" loading="lazy" decoding="async" (error)="failed.set(true)" />
    } @else {
      <span class="placeholder" aria-hidden="true">{{ name() }}</span>
    }
  `,
  styles: `
    :host {
      display: block;
      aspect-ratio: 2 / 3;
      border-radius: var(--radius-sm);
      overflow: hidden;
      background: var(--color-surface);
      border: 1px solid var(--color-rule);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .placeholder {
      display: grid;
      place-items: center;
      height: 100%;
      padding: var(--space-2);
      color: var(--color-text-muted);
      font-family: var(--font-serif);
      font-size: var(--text-sm);
      text-align: center;
    }
  `,
})
export class Poster {
  readonly path = input<string | null>(null);
  readonly name = input.required<string>();
  readonly size = input<PosterSize>('w342');

  protected readonly src = computed(() => posterUrl(this.path(), this.size()));
  protected readonly failed = linkedSignal({ source: this.src, computation: () => false });
}
