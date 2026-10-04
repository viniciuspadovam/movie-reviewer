import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Auth } from '../../core/auth';
import { errorMessage } from '../../core/errors';
import { Seo } from '../../core/seo';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page login">
      <h1>Entrar</h1>
      <form (submit)="submit($event, username.value, password.value)">
        <div class="field">
          <label for="username">Usuário</label>
          <input #username id="username" class="input" autocomplete="username" required autofocus />
        </div>
        <div class="field">
          <label for="password">Senha</label>
          <input
            #password
            id="password"
            class="input"
            type="password"
            autocomplete="current-password"
            required
          />
        </div>
        @if (error(); as error) {
          <p class="error-text" role="alert">{{ error }}</p>
        }
        <button type="submit" class="button button-primary" [disabled]="submitting()">
          {{ submitting() ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>
    </section>
  `,
  styles: `
    .login {
      max-width: 22rem;
    }
    h1 {
      font-size: var(--text-2xl);
      margin-bottom: var(--space-5);
    }
    form {
      display: grid;
      gap: var(--space-4);
    }
  `,
})
export class Login implements OnInit {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  readonly redirect = input<string>();

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  constructor() {
    inject(Seo).set('Entrar');
  }

  async ngOnInit(): Promise<void> {
    // Also fetches the XSRF cookie the login POST needs.
    if (await this.auth.ensureLoaded()) {
      await this.router.navigateByUrl(this.target());
    }
  }

  protected async submit(event: Event, username: string, password: string): Promise<void> {
    event.preventDefault();
    if (!username || !password) {
      this.error.set('Informe usuário e senha.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.login(username, password);
      await this.router.navigateByUrl(this.target());
    } catch (error) {
      this.error.set(errorMessage(error, 'Não foi possível entrar.'));
    } finally {
      this.submitting.set(false);
    }
  }

  private target(): string {
    const redirect = this.redirect();
    return redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/admin';
  }
}
