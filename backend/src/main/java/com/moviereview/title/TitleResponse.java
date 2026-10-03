package com.moviereview.title;

import java.util.Comparator;
import java.util.List;

public record TitleResponse(
		Long id,
		String slug,
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
		List<GenreResponse> genres) {

	public static TitleResponse from(Title title) {
		List<GenreResponse> genres = title.getGenres()
			.stream()
			.map(GenreResponse::from)
			.sorted(Comparator.comparing(GenreResponse::name))
			.toList();
		return new TitleResponse(title.getId(), title.getSlug(), title.getTmdbId(), title.getMediaType(),
				title.getName(), title.getOriginalName(), title.getReleaseYear(), title.getOverview(),
				title.getPosterPath(), title.getBackdropPath(), title.getRuntimeMinutes(), title.getSeasonCount(),
				genres);
	}

}
