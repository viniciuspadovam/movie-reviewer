import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/public/home').then((m) => m.Home),
  },
  {
    path: 'title/:slug',
    loadComponent: () => import('./features/public/title-detail').then((m) => m.TitleDetail),
  },
  {
    path: 'review/:id',
    loadComponent: () => import('./features/public/review-detail').then((m) => m.ReviewDetailPage),
  },
  {
    path: 'search',
    loadComponent: () => import('./features/public/search').then((m) => m.Search),
  },
  {
    path: '**',
    title: 'Página não encontrada · Pós-Créditos',
    loadComponent: () => import('./features/public/not-found').then((m) => m.NotFound),
  },
];
