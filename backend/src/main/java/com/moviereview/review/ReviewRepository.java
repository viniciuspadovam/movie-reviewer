package com.moviereview.review;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByTitleIdAndStatusOrderByWatchedOnAscCreatedAtAsc(Long titleId, ReviewStatus status);

	@EntityGraph(attributePaths = "title")
	Page<Review> findByStatusOrderByPublishedAtDesc(ReviewStatus status, Pageable pageable);

	@EntityGraph(attributePaths = "title")
	Optional<Review> findByIdAndStatus(Long id, ReviewStatus status);

	@EntityGraph(attributePaths = "title")
	Page<Review> findAllByOrderByUpdatedAtDesc(Pageable pageable);

	@EntityGraph(attributePaths = "title")
	Page<Review> findByStatusOrderByUpdatedAtDesc(ReviewStatus status, Pageable pageable);

	boolean existsByTitleId(Long titleId);

	boolean existsByTitleIdAndStatus(Long titleId, ReviewStatus status);

}
