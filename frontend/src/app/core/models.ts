export type MediaType = 'MOVIE' | 'SERIES';

export type ReviewStatus = 'DRAFT' | 'PUBLISHED';

export type SearchSort = 'RECENT' | 'RATING' | 'TITLE';

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Genre {
  id: number;
  name: string;
}

export interface TitleSummary {
  id: number;
  slug: string;
  mediaType: MediaType;
  name: string;
  releaseYear: number | null;
  posterPath: string | null;
}

export interface Title extends TitleSummary {
  tmdbId: number;
  originalName: string | null;
  overview: string | null;
  backdropPath: string | null;
  runtimeMinutes: number | null;
  seasonCount: number | null;
  genres: Genre[];
}

export interface ReviewSummary {
  id: number;
  title: TitleSummary;
  watchedOn: string;
  rating: number;
  excerpt: string;
  hasSpoilers: boolean;
  publishedAt: string;
  editedAt: string | null;
}

export interface SessionLink {
  id: number;
  sessionNumber: number;
  watchedOn: string;
  rating: number;
}

export interface ReviewDetail {
  id: number;
  title: TitleSummary;
  sessionNumber: number;
  watchedOn: string;
  rating: number;
  content: string;
  hasSpoilers: boolean;
  publishedAt: string;
  editedAt: string | null;
  sessions: SessionLink[];
}

export interface Session {
  id: number;
  sessionNumber: number;
  watchedOn: string;
  rating: number;
  ratingDelta: number | null;
  excerpt: string;
  hasSpoilers: boolean;
  publishedAt: string;
  editedAt: string | null;
}

export interface TitlePage {
  title: Title;
  currentRating: number;
  sessions: Session[];
}

export interface TitleSearchItem {
  id: number;
  slug: string;
  mediaType: MediaType;
  name: string;
  releaseYear: number | null;
  posterPath: string | null;
  currentRating: number;
  sessionCount: number;
  lastPublishedAt: string;
}

export interface SearchFilters {
  q?: string;
  type?: MediaType;
  genre?: number;
  minRating?: number;
  maxRating?: number;
  year?: number;
  sort?: SearchSort;
  page?: number;
}

export interface AdminReview {
  id: number;
  title: TitleSummary;
  watchedOn: string;
  rating: number;
  content: string;
  hasSpoilers: boolean;
  status: ReviewStatus;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewRequest {
  watchedOn: string;
  rating: number;
  content: string;
  hasSpoilers: boolean;
  status: ReviewStatus;
}

export interface TmdbSearchResult {
  tmdbId: number;
  mediaType: MediaType;
  name: string;
  originalName: string | null;
  releaseYear: number | null;
  overview: string | null;
  posterPath: string | null;
}

export interface User {
  username: string;
}

export interface ProblemDetail {
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
