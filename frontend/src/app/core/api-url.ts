import { environment } from '../../environments/environment';

const API_PREFIX = `${environment.apiBaseUrl}/api/v1`;

export function apiUrl(path: string): string {
  return `${API_PREFIX}${path}`;
}

export function isApiUrl(url: string): boolean {
  return url.startsWith(API_PREFIX);
}
