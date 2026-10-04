import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '**',
    title: 'Página não encontrada · Pós-Créditos',
    loadComponent: () => import('./features/public/not-found').then((m) => m.NotFound),
  },
];
