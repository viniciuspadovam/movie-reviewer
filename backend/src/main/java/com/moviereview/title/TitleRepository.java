package com.moviereview.title;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TitleRepository extends JpaRepository<Title, Long> {

	@EntityGraph(attributePaths = "genres")
	Optional<Title> findBySlug(String slug);

	Optional<Title> findByTmdbIdAndMediaType(Integer tmdbId, MediaType mediaType);

	boolean existsBySlug(String slug);

}
