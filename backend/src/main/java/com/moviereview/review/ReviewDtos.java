package com.moviereview.review;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.moviereview.common.MarkdownExcerpt;
import com.moviereview.title.TitleResponse;
import com.moviereview.title.TitleSummary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReviewDtos {

	private ReviewDtos() {
	}

	public record ReviewRequest(
			@NotNull(message = "Informe a data em que assistiu.") LocalDate watchedOn,
			@NotNull(message = "Informe a nota.") @Min(value = Review.MIN_RATING,
					message = "A nota mínima é meia estrela.") @Max(value = Review.MAX_RATING,
							message = "A nota máxima é 5 estrelas.") Integer rating,
			@Size(max = 100_000, message = "O texto é longo demais.") String content,
			boolean hasSpoilers,
			@NotNull(message = "Informe o status.") ReviewStatus status) {

		ReviewContent toContent() {
			return new ReviewContent(watchedOn, rating, content, hasSpoilers, status);
		}

	}

	public record AdminReviewResponse(Long id, TitleSummary title, LocalDate watchedOn, int rating, String content,
			boolean hasSpoilers, ReviewStatus status, Instant publishedAt, Instant createdAt, Instant updatedAt) {

		static AdminReviewResponse from(Review review) {
			return new AdminReviewResponse(review.getId(), TitleSummary.from(review.getTitle()), review.getWatchedOn(),
					review.getRating(), review.getContent(), review.hasSpoilers(), review.getStatus(),
					review.getPublishedAt(), review.getCreatedAt(), review.getUpdatedAt());
		}

	}

	public record ReviewSummaryResponse(Long id, TitleSummary title, LocalDate watchedOn, int rating, String excerpt,
			boolean hasSpoilers, Instant publishedAt, Instant editedAt) {

		static ReviewSummaryResponse from(Review review) {
			return new ReviewSummaryResponse(review.getId(), TitleSummary.from(review.getTitle()),
					review.getWatchedOn(), review.getRating(), MarkdownExcerpt.of(review.getContent()),
					review.hasSpoilers(), review.getPublishedAt(), editedAtOf(review));
		}

	}

	public record ReviewDetailResponse(Long id, TitleSummary title, int sessionNumber, LocalDate watchedOn, int rating,
			String content, boolean hasSpoilers, Instant publishedAt, Instant editedAt, List<SessionLink> sessions) {
	}

	public record SessionLink(Long id, int sessionNumber, LocalDate watchedOn, int rating) {
	}

	public record SessionResponse(Long id, int sessionNumber, LocalDate watchedOn, int rating, Integer ratingDelta,
			String excerpt, boolean hasSpoilers, Instant publishedAt, Instant editedAt) {
	}

	public record TitlePageResponse(TitleResponse title, int currentRating, List<SessionResponse> sessions) {
	}

	static Instant editedAtOf(Review review) {
		return review.getUpdatedAt().isAfter(review.getPublishedAt()) ? review.getUpdatedAt() : null;
	}

}
