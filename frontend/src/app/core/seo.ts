import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

const SITE_NAME = 'Pós-Créditos';
const DEFAULT_DESCRIPTION = 'Reviews de filmes e séries, sessão por sessão.';

@Injectable({ providedIn: 'root' })
export class Seo {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  set(pageTitle: string | null, description: string | null = null): void {
    this.title.setTitle(pageTitle ? `${pageTitle} · ${SITE_NAME}` : SITE_NAME);
    this.meta.updateTag({ name: 'description', content: description || DEFAULT_DESCRIPTION });
  }
}
