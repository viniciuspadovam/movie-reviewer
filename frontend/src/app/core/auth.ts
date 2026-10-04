import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { catchError, firstValueFrom, map, of, tap } from 'rxjs';

import { apiUrl } from './api-url';
import { User } from './models';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<User | null | undefined>(undefined);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => !!this.currentUser());

  async ensureLoaded(): Promise<boolean> {
    if (this.currentUser() === undefined) {
      await this.refresh();
    }
    return this.isLoggedIn();
  }

  refresh(): Promise<User | null> {
    return firstValueFrom(
      this.http.get<User>(apiUrl('/auth/me')).pipe(
        catchError(() => of(null)),
        tap((user) => this.currentUser.set(user)),
      ),
    );
  }

  login(username: string, password: string): Promise<User> {
    return firstValueFrom(
      this.http
        .post<User>(apiUrl('/auth/login'), { username, password })
        .pipe(tap((user) => this.currentUser.set(user))),
    );
  }

  logout(): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(apiUrl('/auth/logout'), null).pipe(
        catchError(() => of(undefined)),
        map(() => this.currentUser.set(null)),
      ),
    );
  }
}

export const authGuard: CanMatchFn = async (_route, segments) => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (await auth.ensureLoaded()) {
    return true;
  }
  const redirect = '/' + segments.map((segment) => segment.path).join('/');
  return router.createUrlTree(['/login'], { queryParams: { redirect } });
};
