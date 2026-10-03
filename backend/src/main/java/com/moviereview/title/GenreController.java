package com.moviereview.title;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/genres")
class GenreController {

	private final GenreRepository genreRepository;

	GenreController(GenreRepository genreRepository) {
		this.genreRepository = genreRepository;
	}

	@GetMapping
	List<GenreResponse> list() {
		return genreRepository.findAllInUse().stream().map(GenreResponse::from).toList();
	}

}
