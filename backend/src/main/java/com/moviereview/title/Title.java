package com.moviereview.title;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Title {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer tmdbId;

	@Enumerated(EnumType.STRING)
	private MediaType mediaType;

	private String name;

	private String originalName;

	private Short releaseYear;

	private String overview;

	private String posterPath;

	private String backdropPath;

	private Short runtimeMinutes;

	private Short seasonCount;

	@Column(updatable = false)
	private String slug;

	@Column(updatable = false)
	private Instant createdAt;

	@ManyToMany
	@JoinTable(name = "title_genre", joinColumns = @JoinColumn(name = "title_id"),
			inverseJoinColumns = @JoinColumn(name = "genre_id"))
	private Set<Genre> genres = new HashSet<>();

	protected Title() {
	}

	public Title(TitleMetadata metadata, Set<Genre> genres, String slug, Instant createdAt) {
		this.tmdbId = metadata.tmdbId();
		this.mediaType = metadata.mediaType();
		this.name = metadata.name();
		this.originalName = metadata.originalName();
		this.releaseYear = metadata.releaseYear();
		this.overview = metadata.overview();
		this.posterPath = metadata.posterPath();
		this.backdropPath = metadata.backdropPath();
		this.runtimeMinutes = metadata.runtimeMinutes();
		this.seasonCount = metadata.seasonCount();
		this.genres = new HashSet<>(genres);
		this.slug = slug;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Integer getTmdbId() {
		return tmdbId;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public String getName() {
		return name;
	}

	public String getOriginalName() {
		return originalName;
	}

	public Short getReleaseYear() {
		return releaseYear;
	}

	public String getOverview() {
		return overview;
	}

	public String getPosterPath() {
		return posterPath;
	}

	public String getBackdropPath() {
		return backdropPath;
	}

	public Short getRuntimeMinutes() {
		return runtimeMinutes;
	}

	public Short getSeasonCount() {
		return seasonCount;
	}

	public String getSlug() {
		return slug;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Set<Genre> getGenres() {
		return Set.copyOf(genres);
	}

}
