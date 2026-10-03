package com.moviereview.search;

import java.time.Instant;

import com.moviereview.title.MediaType;

public record TitleSearchItem(
		Long id,
		String slug,
		MediaType mediaType,
		String name,
		Short releaseYear,
		String posterPath,
		int currentRating,
		int sessionCount,
		Instant lastPublishedAt) {
}
