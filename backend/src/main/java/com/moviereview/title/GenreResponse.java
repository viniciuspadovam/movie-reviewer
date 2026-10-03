package com.moviereview.title;

public record GenreResponse(Integer id, String name) {

	public static GenreResponse from(Genre genre) {
		return new GenreResponse(genre.getId(), genre.getName());
	}

}
