package com.moviereview.title;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
class AdminTitleController {

	private final TitleService titleService;

	AdminTitleController(TitleService titleService) {
		this.titleService = titleService;
	}

	@GetMapping("/tmdb/search")
	List<TitleSearchResult> searchTmdb(
			@RequestParam("q") @NotBlank(message = "Informe o que buscar.") @Size(max = 200) String query,
			@RequestParam("type") MediaType mediaType) {
		return titleService.searchExternal(query.strip(), mediaType);
	}

	@PostMapping("/titles")
	ResponseEntity<TitleResponse> importTitle(@Valid @RequestBody ImportTitleRequest request) {
		TitleService.ImportResult result = titleService.importTitle(request.tmdbId(), request.mediaType());
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.title());
	}

	@GetMapping("/titles/{id}")
	TitleResponse getTitle(@PathVariable long id) {
		return titleService.get(id);
	}

	@DeleteMapping("/titles/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteTitle(@PathVariable long id, @RequestParam(defaultValue = "false") boolean cascade) {
		titleService.delete(id, cascade);
	}

	record ImportTitleRequest(@NotNull(message = "Informe o id do TMDB.") @Positive Integer tmdbId,
			@NotNull(message = "Informe o tipo da obra.") MediaType mediaType) {
	}

}
