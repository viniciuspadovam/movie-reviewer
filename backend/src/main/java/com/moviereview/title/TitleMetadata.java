package com.moviereview.title;

import java.util.List;

public record TitleMetadata(
		Integer tmdbId,
		MediaType mediaType,
		String name,
		String originalName,
		Short releaseYear,
		String overview,
		String posterPath,
		String backdropPath,
		Short runtimeMinutes,
		Short seasonCount,
		List<GenreMetadata> genres) {

	public record GenreMetadata(Integer id, String name) {
	}

}
