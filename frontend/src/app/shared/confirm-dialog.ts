import { ChangeDetectionStrategy, Component, ElementRef, signal, viewChild } from '@angular/core';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel: string;
}

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialog
      aria-labelledby="confirm-title"
      (close)="settle(dialog.returnValue === 'confirm')"
    >
      @if (options(); as options) {
        <form method="dialog">
          <h2 id="confirm-title">{{ options.title }}</h2>
          <p>{{ options.message }}</p>
          <div class="actions">
            <button type="submit" class="button" value="cancel" autofocus>Cancelar</button>
            <button type="submit" class="button button-primary" value="confirm">
              {{ options.confirmLabel }}
            </button>
          </div>
        </form>
      }
    </dialog>
  `,
  styles: `
    dialog {
      width: min(100% - 2 * var(--space-4), 28rem);
      padding: var(--space-5);
      border: 1px solid var(--color-rule);
      border-radius: var(--radius-md);
      background: var(--color-surface);
      color: var(--color-text);
    }
    dialog::backdrop {
      background: rgb(0 0 0 / 0.6);
    }
    h2 {
      font-size: var(--text-xl);
      margin-bottom: var(--space-3);
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--space-2);
      margin-top: var(--space-5);
    }
  `,
})
export class ConfirmDialog {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly options = signal<ConfirmOptions | null>(null);
  private resolve: ((confirmed: boolean) => void) | null = null;

  ask(options: ConfirmOptions): Promise<boolean> {
    this.options.set(options);
    this.dialog().nativeElement.returnValue = '';
    this.dialog().nativeElement.showModal();
    return new Promise((resolve) => (this.resolve = resolve));
  }

  protected settle(confirmed: boolean): void {
    this.resolve?.(confirmed);
    this.resolve = null;
  }
}
