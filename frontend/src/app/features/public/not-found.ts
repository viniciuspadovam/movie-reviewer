import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <h1>Essa sessão não existe</h1>
      <p class="lead">O endereço pode ter mudado ou a review ainda não foi publicada.</p>
      <p><a routerLink="/">Voltar para as últimas reviews</a> ou <a routerLink="/search">buscar uma obra</a>.</p>
    </section>
  `,
  styles: `
    h1 {
      font-size: var(--text-2xl);
      margin-bottom: var(--space-4);
    }
    .lead {
      font-family: var(--font-serif);
      font-size: var(--text-lg);
      color: var(--color-text-muted);
      margin-bottom: var(--space-4);
    }
  `,
})
export class NotFound {}
