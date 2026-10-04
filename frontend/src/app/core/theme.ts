import { DOCUMENT, Injectable, inject, signal } from '@angular/core';

export type ThemeName = 'dark' | 'light';

const STORAGE_KEY = 'theme';
const THEME_COLORS: Record<ThemeName, string> = { dark: '#1a1210', light: '#edebe6' };

@Injectable({ providedIn: 'root' })
export class Theme {
  private readonly document = inject(DOCUMENT);
  private readonly current = signal<ThemeName>(
    this.document.documentElement.dataset['theme'] === 'light' ? 'light' : 'dark',
  );

  readonly name = this.current.asReadonly();

  toggle(): void {
    const next: ThemeName = this.current() === 'dark' ? 'light' : 'dark';
    this.current.set(next);
    const root = this.document.documentElement;
    if (next === 'light') {
      root.dataset['theme'] = 'light';
    } else {
      delete root.dataset['theme'];
    }
    this.document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[next]);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Storage may be unavailable (private mode); the theme still applies for this visit.
    }
  }
}
