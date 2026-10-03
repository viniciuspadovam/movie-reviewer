package com.moviereview.search;

public enum SearchSort {

	RECENT("s.last_published_at DESC"),
	RATING("c.current_rating DESC, s.last_published_at DESC"),
	TITLE("t.name ASC");

	private final String orderBy;

	SearchSort(String orderBy) {
		this.orderBy = orderBy;
	}

	String orderBy() {
		return orderBy;
	}

}
