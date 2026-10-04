import { ChangeDetectionStrategy, Component, input, linkedSignal } from '@angular/core';

@Component({
  selector: 'app-spoiler-guard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (hidden()) {
      <div class="guard">
        <p>Esta review contém spoilers.</p>
        <button type="button" class="button" (click)="hidden.set(false)">Mostrar spoiler</button>
      </div>
    } @else {
      <ng-content />
    }
  `,
  styles: `
    .guard {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
      border: 1px dashed var(--color-rule);
      border-radius: var(--radius-md);
      color: var(--color-text-muted);
    }
  `,
})
export class SpoilerGuard {
  readonly hasSpoilers = input.required<boolean>();

  protected readonly hidden = linkedSignal(() => this.hasSpoilers());
}
