import { MediaType } from './models';

const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p';

const DATE_FORMAT = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
});

const DATE_TIME_FORMAT = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

export type PosterSize = 'w185' | 'w342' | 'w500';

export function posterUrl(path: string | null, size: PosterSize = 'w342'): string | null {
  return path ? `${TMDB_IMAGE_BASE}/${size}${path}` : null;
}

export function backdropUrl(path: string | null): string | null {
  return path ? `${TMDB_IMAGE_BASE}/w1280${path}` : null;
}

// Ratings travel as integers 1–10 (half stars); only the UI turns them into stars.
export function starsLabel(rating: number): string {
  return (rating / 2).toLocaleString('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1,
  });
}

export function ratingDeltaLabel(delta: number): string {
  const sign = delta > 0 ? '+' : delta < 0 ? '−' : '±';
  return `${sign}${starsLabel(Math.abs(delta))}`;
}

export function formatDate(isoDate: string): string {
  return DATE_FORMAT.format(new Date(isoDate.length === 10 ? `${isoDate}T00:00:00Z` : isoDate));
}

export function formatDateTime(isoInstant: string): string {
  return DATE_TIME_FORMAT.format(new Date(isoInstant));
}

export function mediaTypeLabel(mediaType: MediaType): string {
  return mediaType === 'MOVIE' ? 'Filme' : 'Série';
}

export function kindAndYear(mediaType: MediaType, releaseYear: number | null): string {
  return releaseYear ? `${mediaTypeLabel(mediaType)} de ${releaseYear}` : mediaTypeLabel(mediaType);
}

export function nameWithYear(name: string, releaseYear: number | null): string {
  return releaseYear ? `${name} (${releaseYear})` : name;
}

export function sessionLabel(sessionNumber: number): string {
  return `${sessionNumber}ª sessão`;
}

export function todayIso(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().substring(0, 10);
}
