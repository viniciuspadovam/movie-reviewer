import {
  formatDate,
  kindAndYear,
  nameWithYear,
  posterUrl,
  ratingDeltaLabel,
  sessionLabel,
  starsLabel,
} from './format';

describe('format', () => {
  it('turns half-star integers into a Portuguese star count', () => {
    expect(starsLabel(9)).toBe('4,5');
    expect(starsLabel(10)).toBe('5');
    expect(starsLabel(1)).toBe('0,5');
  });

  it('labels rating changes between sessions', () => {
    expect(ratingDeltaLabel(3)).toBe('+1,5');
    expect(ratingDeltaLabel(-2)).toBe('−1');
    expect(ratingDeltaLabel(0)).toBe('±0');
  });

  it('formats plain dates without shifting the day', () => {
    expect(formatDate('2026-09-12')).toContain('12');
    expect(formatDate('2026-09-12')).toContain('2026');
  });

  it('builds TMDB image urls only when there is a path', () => {
    expect(posterUrl('/abc.jpg', 'w185')).toBe('https://image.tmdb.org/t/p/w185/abc.jpg');
    expect(posterUrl(null)).toBeNull();
  });

  it('numbers sessions in Portuguese', () => {
    expect(sessionLabel(2)).toBe('2ª sessão');
  });

  it('describes titles with and without a release year', () => {
    expect(kindAndYear('MOVIE', 1999)).toBe('Filme de 1999');
    expect(kindAndYear('SERIES', null)).toBe('Série');
    expect(nameWithYear('Dark', 2017)).toBe('Dark (2017)');
    expect(nameWithYear('Dark', null)).toBe('Dark');
  });
});
