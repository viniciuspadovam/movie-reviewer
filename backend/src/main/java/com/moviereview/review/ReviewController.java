package com.moviereview.review;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moviereview.common.PageResponse;
import com.moviereview.review.ReviewDtos.ReviewDetailResponse;
import com.moviereview.review.ReviewDtos.ReviewSummaryResponse;
import com.moviereview.review.ReviewDtos.TitlePageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
class ReviewController {

	private final ReviewQueryService reviewQueryService;

	ReviewController(ReviewQueryService reviewQueryService) {
		this.reviewQueryService = reviewQueryService;
	}

	@GetMapping("/reviews/latest")
	PageResponse<ReviewSummaryResponse> latest(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
		return reviewQueryService.latest(PageRequest.of(page, size));
	}

	@GetMapping("/reviews/{id}")
	ReviewDetailResponse get(@PathVariable long id) {
		return reviewQueryService.getPublished(id);
	}

	@GetMapping("/titles/{slug}")
	TitlePageResponse titlePage(@PathVariable String slug) {
		return reviewQueryService.titlePage(slug);
	}

}
