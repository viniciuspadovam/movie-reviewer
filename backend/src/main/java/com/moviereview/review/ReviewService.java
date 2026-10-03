package com.moviereview.review;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereview.common.NotFoundException;
import com.moviereview.common.PageResponse;
import com.moviereview.review.ReviewDtos.AdminReviewResponse;
import com.moviereview.review.ReviewDtos.ReviewRequest;
import com.moviereview.title.Title;
import com.moviereview.title.TitleRepository;

@Service
public class ReviewService {

	private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

	private final ReviewRepository reviewRepository;

	private final TitleRepository titleRepository;

	private final Clock clock;

	ReviewService(ReviewRepository reviewRepository, TitleRepository titleRepository, Clock clock) {
		this.reviewRepository = reviewRepository;
		this.titleRepository = titleRepository;
		this.clock = clock;
	}

	@Transactional
	public AdminReviewResponse create(long titleId, ReviewRequest request) {
		Title title = titleRepository.findById(titleId)
			.orElseThrow(() -> new NotFoundException("Obra não encontrada."));
		Review review = reviewRepository
			.save(new Review(title, request.toContent(), LocalDate.now(clock), clock.instant()));
		log.info("Review {} criada para a obra {} com status {}", review.getId(), titleId, review.getStatus());
		return AdminReviewResponse.from(review);
	}

	@Transactional
	public AdminReviewResponse update(long id, ReviewRequest request) {
		Review review = findWithTitle(id);
		review.update(request.toContent(), LocalDate.now(clock), clock.instant());
		log.info("Review {} atualizada com status {}", id, review.getStatus());
		return AdminReviewResponse.from(review);
	}

	@Transactional
	public void delete(long id) {
		Review review = reviewRepository.findById(id).orElseThrow(ReviewService::notFound);
		reviewRepository.delete(review);
		log.info("Review {} excluída", id);
	}

	@Transactional(readOnly = true)
	public AdminReviewResponse get(long id) {
		return AdminReviewResponse.from(findWithTitle(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminReviewResponse> list(ReviewStatus status, Pageable pageable) {
		Page<Review> page = status == null ? reviewRepository.findAllByOrderByUpdatedAtDesc(pageable)
				: reviewRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
		return PageResponse.from(page, AdminReviewResponse::from);
	}

	static NotFoundException notFound() {
		return new NotFoundException("Review não encontrada.");
	}

	private Review findWithTitle(long id) {
		return reviewRepository.findWithTitleById(id).orElseThrow(ReviewService::notFound);
	}

}
