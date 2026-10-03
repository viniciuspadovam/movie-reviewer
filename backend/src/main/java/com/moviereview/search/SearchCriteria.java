package com.moviereview.search;

import com.moviereview.common.BusinessRuleException;
import com.moviereview.title.MediaType;

public record SearchCriteria(
		String query,
		MediaType mediaType,
		Integer genreId,
		Integer minRating,
		Integer maxRating,
		Integer year,
		SearchSort sort,
		int page,
		int size) {

	public SearchCriteria {
		if (minRating != null && maxRating != null && minRating > maxRating) {
			throw new BusinessRuleException("A nota mínima não pode ser maior que a máxima.");
		}
	}

	public boolean hasQuery() {
		return query != null && !query.isBlank();
	}

}
