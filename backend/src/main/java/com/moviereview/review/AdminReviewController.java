package com.moviereview.review;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.moviereview.common.PageResponse;
import com.moviereview.review.ReviewDtos.AdminReviewResponse;
import com.moviereview.review.ReviewDtos.ReviewRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/admin")
class AdminReviewController {

	private final ReviewService reviewService;

	AdminReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PostMapping("/titles/{titleId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	AdminReviewResponse create(@PathVariable long titleId, @Valid @RequestBody ReviewRequest request) {
		return reviewService.create(titleId, request);
	}

	@GetMapping("/reviews")
	PageResponse<AdminReviewResponse> list(@RequestParam(required = false) ReviewStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return reviewService.list(status, PageRequest.of(page, size));
	}

	@GetMapping("/reviews/{id}")
	AdminReviewResponse get(@PathVariable long id) {
		return reviewService.get(id);
	}

	@PutMapping("/reviews/{id}")
	AdminReviewResponse update(@PathVariable long id, @Valid @RequestBody ReviewRequest request) {
		return reviewService.update(id, request);
	}

	@DeleteMapping("/reviews/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id) {
		reviewService.delete(id);
	}

}
