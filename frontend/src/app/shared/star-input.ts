import { ChangeDetectionStrategy, Component, model } from '@angular/core';

import { starsLabel } from '../core/format';

const VALUES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// Ten native radios (half stars) keep keyboard and screen reader support; the SVG is only the visual layer.
@Component({
  selector: 'app-star-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fieldset class="star-input">
      <legend>Nota</legend>
      <div class="scale" (mouseleave)="hovered = null">
        @for (value of values; track value) {
          <label
            [class.left]="value % 2 === 1"
            [class.on]="(hovered ?? rating() ?? 0) >= value"
            (mouseenter)="hovered = value"
          >
            <input
              type="radio"
              name="rating"
              class="visually-hidden"
              [value]="value"
              [checked]="rating() === value"
              (change)="rating.set(value)"
            />
            <span class="visually-hidden">{{ label(value) }} {{ value === 2 ? 'estrela' : 'estrelas' }}</span>
          </label>
        }
      </div>
      <span class="value" aria-hidden="true">{{ rating() ? label(rating()!) : '—' }}</span>
    </fieldset>
  `,
  styles: `
    .star-input {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      margin: 0;
      padding: 0;
      border: 0;
    }
    legend {
      float: left;
      margin-right: var(--space-3);
      color: var(--color-text-muted);
      font-size: var(--text-sm);
    }
    .scale {
      display: grid;
      grid-template-columns: repeat(10, 0.9rem);
    }
    label {
      position: relative;
      height: 1.8rem;
      cursor: pointer;
      background: var(--color-rule);
      mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 2.8l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.2l-5.7 3.1 1.2-6.4-4.7-4.4 6.4-.8z'/%3E%3C/svg%3E")
        no-repeat;
      mask-size: 1.8rem 1.8rem;
    }
    label.left {
      mask-position: 0 0;
    }
    label:not(.left) {
      mask-position: -0.9rem 0;
    }
    label.on {
      background: var(--color-accent);
    }
    label:has(input:focus-visible) {
      outline: 2px solid var(--color-focus);
      outline-offset: 2px;
    }
    .value {
      min-width: 2.5ch;
      font-weight: 600;
      font-variant-numeric: tabular-nums;
    }
  `,
})
export class StarInput {
  readonly rating = model<number | null>(null);

  protected readonly values = VALUES;
  protected hovered: number | null = null;
  protected readonly label = starsLabel;
}
