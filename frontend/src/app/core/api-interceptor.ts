import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { DOCUMENT, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { isApiUrl } from './api-url';

const XSRF_COOKIE = 'XSRF-TOKEN';
const XSRF_HEADER = 'X-XSRF-TOKEN';
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

// Angular's built-in XSRF support skips absolute URLs, and in production the API lives on its own subdomain.
export const apiInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiUrl(request.url)) {
    return next(request);
  }
  const document = inject(DOCUMENT);
  const router = inject(Router);
  let apiRequest = request.clone({ withCredentials: true });
  const token = readCookie(document, XSRF_COOKIE);
  if (token && !SAFE_METHODS.has(request.method)) {
    apiRequest = apiRequest.clone({ setHeaders: { [XSRF_HEADER]: token } });
  }
  return next(apiRequest).pipe(
    catchError((error: unknown) => {
      const isSessionCheck = request.url.endsWith('/auth/me');
      if (error instanceof HttpErrorResponse && error.status === 401 && !isSessionCheck) {
        if (router.url.startsWith('/admin')) {
          router.navigate(['/login'], { queryParams: { redirect: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};

function readCookie(document: Document, name: string): string | null {
  const prefix = `${name}=`;
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.substring(prefix.length)) : null;
}
