package com.moviereview.title;

public record TitleSummary(Long id, String slug, MediaType mediaType, String name, Short releaseYear,
		String posterPath) {

	public static TitleSummary from(Title title) {
		return new TitleSummary(title.getId(), title.getSlug(), title.getMediaType(), title.getName(),
				title.getReleaseYear(), title.getPosterPath());
	}

}
