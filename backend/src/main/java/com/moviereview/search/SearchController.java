package com.moviereview.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moviereview.common.PageResponse;
import com.moviereview.review.Review;
import com.moviereview.title.MediaType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/titles")
class SearchController {

	private final TitleSearchRepository titleSearchRepository;

	SearchController(TitleSearchRepository titleSearchRepository) {
		this.titleSearchRepository = titleSearchRepository;
	}

	@GetMapping
	PageResponse<TitleSearchItem> search(
			@RequestParam(name = "q", required = false) @Size(max = 200, message = "Busca longa demais.") String query,
			@RequestParam(name = "type", required = false) MediaType mediaType,
			@RequestParam(name = "genre", required = false) Integer genreId,
			@RequestParam(required = false) @Min(value = Review.MIN_RATING, message = "Nota mínima inválida.") @Max(
					value = Review.MAX_RATING, message = "Nota mínima inválida.") Integer minRating,
			@RequestParam(required = false) @Min(value = Review.MIN_RATING, message = "Nota máxima inválida.") @Max(
					value = Review.MAX_RATING, message = "Nota máxima inválida.") Integer maxRating,
			@RequestParam(required = false) @Min(value = 1870, message = "Ano inválido.") @Max(value = 2200,
					message = "Ano inválido.") Integer year,
			@RequestParam(defaultValue = "RECENT") SearchSort sort,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		String normalizedQuery = query == null ? null : query.strip();
		return titleSearchRepository.search(new SearchCriteria(normalizedQuery, mediaType, genreId, minRating,
				maxRating, year, sort, page, size));
	}

}
