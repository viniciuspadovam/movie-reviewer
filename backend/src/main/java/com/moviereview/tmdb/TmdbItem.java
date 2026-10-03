package com.moviereview.tmdb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// TMDB uses "title"/"release_date" for movies and "name"/"first_air_date" for series; one shape covers both.
record TmdbItem(
		int id,
		String title,
		String name,
		@JsonProperty("original_title") String originalTitle,
		@JsonProperty("original_name") String originalName,
		@JsonProperty("release_date") String releaseDate,
		@JsonProperty("first_air_date") String firstAirDate,
		String overview,
		@JsonProperty("poster_path") String posterPath,
		@JsonProperty("backdrop_path") String backdropPath,
		Integer runtime,
		@JsonProperty("number_of_seasons") Integer numberOfSeasons,
		List<TmdbGenre> genres) {

	record TmdbGenre(int id, String name) {
	}

	record SearchPage(List<TmdbItem> results) {
	}

	String displayName() {
		return title != null ? title : name;
	}

	String displayOriginalName() {
		return originalTitle != null ? originalTitle : originalName;
	}

	Short releaseYear() {
		String date = releaseDate != null && !releaseDate.isBlank() ? releaseDate : firstAirDate;
		if (date == null || date.length() < 4) {
			return null;
		}
		return Short.valueOf(date.substring(0, 4));
	}

}
