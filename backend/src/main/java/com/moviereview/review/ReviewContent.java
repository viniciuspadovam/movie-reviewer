package com.moviereview.review;

import java.time.LocalDate;

public record ReviewContent(LocalDate watchedOn, int rating, String content, boolean hasSpoilers,
		ReviewStatus status) {
}
