package com.moviereview.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.moviereview.common.BusinessRuleException;

class ReviewTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

	private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

	@Test
	void draftDoesNotSetPublishedAt() {
		Review review = new Review(null, content(ReviewStatus.DRAFT, ""), TODAY, NOW);

		assertThat(review.getPublishedAt()).isNull();
		assertThat(review.getCreatedAt()).isEqualTo(NOW);
		assertThat(review.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void publishingSetsPublishedAtOnlyTheFirstTime() {
		Review review = new Review(null, content(ReviewStatus.PUBLISHED, "Ótimo"), TODAY, NOW);
		Instant later = NOW.plusSeconds(3600);

		review.update(content(ReviewStatus.DRAFT, "Ótimo"), TODAY, later);
		review.update(content(ReviewStatus.PUBLISHED, "Ótimo"), TODAY, later.plusSeconds(60));

		assertThat(review.getPublishedAt()).isEqualTo(NOW);
		assertThat(review.getUpdatedAt()).isEqualTo(later.plusSeconds(60));
	}

	@Test
	void rejectsPublishingWithoutText() {
		assertThatThrownBy(() -> new Review(null, content(ReviewStatus.PUBLISHED, "   "), TODAY, NOW))
			.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsWatchedOnInTheFuture() {
		ReviewContent future = new ReviewContent(TODAY.plusDays(1), 8, "x", false, ReviewStatus.DRAFT);

		assertThatThrownBy(() -> new Review(null, future, TODAY, NOW)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsRatingOutOfRange() {
		ReviewContent zero = new ReviewContent(TODAY, 0, "x", false, ReviewStatus.DRAFT);
		ReviewContent eleven = new ReviewContent(TODAY, 11, "x", false, ReviewStatus.DRAFT);

		assertThatThrownBy(() -> new Review(null, zero, TODAY, NOW)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> new Review(null, eleven, TODAY, NOW)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void stripsContent() {
		Review review = new Review(null, content(ReviewStatus.DRAFT, "  texto \n"), TODAY, NOW);

		assertThat(review.getContent()).isEqualTo("texto");
	}

	private static ReviewContent content(ReviewStatus status, String text) {
		return new ReviewContent(TODAY, 9, text, false, status);
	}

}
