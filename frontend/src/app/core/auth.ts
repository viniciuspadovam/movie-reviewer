import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { catchError, firstValueFrom, map, of, tap } from 'rxjs';

import { apiUrl } from './api-url';
import { User } from './models';

// Visitors never log in; remembering that this browser did avoids a 401 session probe on every public page.
const SESSION_HINT_KEY = 'session-hint';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<User | null | undefined>(undefined);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => !!this.currentUser());

  restoreIfRemembered(): void {
    if (readHint()) {
      void this.ensureLoaded();
    }
  }

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
        tap((user) => this.setUser(user)),
      ),
    );
  }

  login(username: string, password: string): Promise<User> {
    return firstValueFrom(
      this.http
        .post<User>(apiUrl('/auth/login'), { username, password })
        .pipe(tap((user) => this.setUser(user))),
    );
  }

  logout(): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(apiUrl('/auth/logout'), null).pipe(
        catchError(() => of(undefined)),
        map(() => this.setUser(null)),
      ),
    );
  }

  private setUser(user: User | null): void {
    this.currentUser.set(user);
    writeHint(user !== null);
  }
}

function readHint(): boolean {
  try {
    return localStorage.getItem(SESSION_HINT_KEY) === '1';
  } catch {
    return false;
  }
}

function writeHint(loggedIn: boolean): void {
  try {
    if (loggedIn) {
      localStorage.setItem(SESSION_HINT_KEY, '1');
    } else {
      localStorage.removeItem(SESSION_HINT_KEY);
    }
  } catch {
    // Without storage the admin just won't see the header shortcuts until visiting /admin.
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
