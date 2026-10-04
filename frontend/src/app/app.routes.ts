import { Routes } from '@angular/router';

import { authGuard } from './core/auth';

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
    path: 'login',
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'admin',
    canMatch: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/admin/admin-reviews').then((m) => m.AdminReviews),
      },
      {
        path: 'new',
        loadComponent: () => import('./features/admin/new-title').then((m) => m.NewTitle),
      },
      {
        path: 'titles/:titleId/reviews/new',
        loadComponent: () => import('./features/admin/review-editor').then((m) => m.ReviewEditor),
      },
      {
        path: 'reviews/:id/edit',
        loadComponent: () => import('./features/admin/review-editor').then((m) => m.ReviewEditor),
      },
    ],
  },
  {
    path: '**',
    title: 'Página não encontrada · Pós-Créditos',
    loadComponent: () => import('./features/public/not-found').then((m) => m.NotFound),
  },
];
