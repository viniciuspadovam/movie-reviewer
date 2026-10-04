import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './models';

export function errorMessage(error: unknown, fallback = 'Algo deu errado. Tente novamente.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Sem conexão com o servidor. Verifique sua internet e tente novamente.';
    }
    const problem = error.error as ProblemDetail | null;
    if (problem?.detail) {
      return problem.detail;
    }
  }
  return fallback;
}

export function fieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse) {
    return (error.error as ProblemDetail | null)?.errors ?? {};
  }
  return {};
}

export function isNotFound(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 404;
}
