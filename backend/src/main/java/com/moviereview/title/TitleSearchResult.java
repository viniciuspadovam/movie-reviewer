package com.moviereview.title;

public record TitleSearchResult(
		int tmdbId,
		MediaType mediaType,
		String name,
		String originalName,
		Short releaseYear,
		String overview,
		String posterPath) {
}
