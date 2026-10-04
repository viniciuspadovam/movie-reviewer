import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { starsLabel } from '../core/format';

type StarFill = 'full' | 'half' | 'empty';

@Component({
  selector: 'app-star-rating',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="stars" role="img" [attr.aria-label]="'Nota ' + label() + ' de 5'">
      @for (fill of fills(); track $index) {
        <svg viewBox="0 0 24 24" aria-hidden="true" [class]="fill">
          <defs>
            <clipPath [id]="clipId + $index"><rect x="0" y="0" width="12" height="24" /></clipPath>
          </defs>
          <path class="outline" [attr.d]="starPath" />
          @if (fill === 'full') {
            <path class="fill" [attr.d]="starPath" />
          } @else if (fill === 'half') {
            <path
              class="fill"
              [attr.d]="starPath"
              [attr.clip-path]="'url(#' + clipId + $index + ')'"
            />
          }
        </svg>
      }
      @if (showNumber()) {
        <span class="number">{{ label() }}</span>
      }
    </span>
  `,
  styles: `
    .stars {
      display: inline-flex;
      align-items: center;
      gap: 1px;
      vertical-align: middle;
    }
    svg {
      width: var(--star-size, 1rem);
      height: var(--star-size, 1rem);
    }
    .outline {
      fill: none;
      stroke: var(--color-accent);
      stroke-width: 1.5;
    }
    .fill {
      fill: var(--color-accent);
    }
    .number {
      margin-left: var(--space-2);
      font-variant-numeric: tabular-nums;
      font-weight: 600;
    }
  `,
})
export class StarRating {
  private static nextId = 0;

  readonly rating = input.required<number>();
  readonly showNumber = input(false);

  protected readonly clipId = `star-half-${StarRating.nextId++}-`;
  protected readonly starPath =
    'M12 2.8l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.2l-5.7 3.1 1.2-6.4-4.7-4.4 6.4-.8z';
  protected readonly label = computed(() => starsLabel(this.rating()));
  protected readonly fills = computed<StarFill[]>(() =>
    [2, 4, 6, 8, 10].map((threshold) =>
      this.rating() >= threshold ? 'full' : this.rating() === threshold - 1 ? 'half' : 'empty',
    ),
  );
}
