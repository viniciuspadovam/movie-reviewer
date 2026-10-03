package com.moviereview.review;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereview.common.MarkdownExcerpt;
import com.moviereview.common.NotFoundException;
import com.moviereview.common.PageResponse;
import com.moviereview.review.ReviewDtos.ReviewDetailResponse;
import com.moviereview.review.ReviewDtos.ReviewSummaryResponse;
import com.moviereview.review.ReviewDtos.SessionLink;
import com.moviereview.review.ReviewDtos.SessionResponse;
import com.moviereview.review.ReviewDtos.TitlePageResponse;
import com.moviereview.title.Title;
import com.moviereview.title.TitleRepository;
import com.moviereview.title.TitleResponse;
import com.moviereview.title.TitleSummary;

@Service
@Transactional(readOnly = true)
public class ReviewQueryService {

	private final ReviewRepository reviewRepository;

	private final TitleRepository titleRepository;

	ReviewQueryService(ReviewRepository reviewRepository, TitleRepository titleRepository) {
		this.reviewRepository = reviewRepository;
		this.titleRepository = titleRepository;
	}

	public PageResponse<ReviewSummaryResponse> latest(Pageable pageable) {
		return PageResponse.from(reviewRepository.findByStatusOrderByPublishedAtDesc(ReviewStatus.PUBLISHED, pageable),
				ReviewSummaryResponse::from);
	}

	public ReviewDetailResponse getPublished(long id) {
		Review review = reviewRepository.findByIdAndStatus(id, ReviewStatus.PUBLISHED)
			.orElseThrow(ReviewService::notFound);
		List<Review> sessions = publishedSessions(review.getTitle().getId());
		List<SessionLink> links = IntStream.range(0, sessions.size())
			.mapToObj(index -> new SessionLink(sessions.get(index).getId(), index + 1,
					sessions.get(index).getWatchedOn(), sessions.get(index).getRating()))
			.toList();
		int sessionNumber = links.stream()
			.filter(link -> link.id().equals(review.getId()))
			.findFirst()
			.map(SessionLink::sessionNumber)
			.orElseThrow();
		return new ReviewDetailResponse(review.getId(), TitleSummary.from(review.getTitle()), sessionNumber,
				review.getWatchedOn(), review.getRating(), review.getContent(), review.hasSpoilers(),
				review.getPublishedAt(), ReviewDtos.editedAtOf(review), links);
	}

	public TitlePageResponse titlePage(String slug) {
		Title title = titleRepository.findBySlug(slug).orElseThrow(ReviewQueryService::titleNotFound);
		List<Review> sessions = publishedSessions(title.getId());
		if (sessions.isEmpty()) {
			throw titleNotFound();
		}
		List<SessionResponse> responses = new ArrayList<>();
		Integer previousRating = null;
		for (int index = 0; index < sessions.size(); index++) {
			Review review = sessions.get(index);
			Integer delta = previousRating == null ? null : review.getRating() - previousRating;
			responses.add(new SessionResponse(review.getId(), index + 1, review.getWatchedOn(), review.getRating(),
					delta, MarkdownExcerpt.of(review.getContent()), review.hasSpoilers(), review.getPublishedAt(),
					ReviewDtos.editedAtOf(review)));
			previousRating = review.getRating();
		}
		return new TitlePageResponse(TitleResponse.from(title), sessions.getLast().getRating(), responses);
	}

	private List<Review> publishedSessions(Long titleId) {
		return reviewRepository.findByTitleIdAndStatusOrderByWatchedOnAscCreatedAtAsc(titleId, ReviewStatus.PUBLISHED);
	}

	private static NotFoundException titleNotFound() {
		return new NotFoundException("Obra não encontrada.");
	}

}
