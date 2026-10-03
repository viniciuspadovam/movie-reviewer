package com.moviereview.search;

import static com.moviereview.title.MediaType.MOVIE;
import static com.moviereview.title.MediaType.SERIES;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.moviereview.IntegrationTest;
import com.moviereview.review.Review;
import com.moviereview.review.ReviewContent;
import com.moviereview.review.ReviewRepository;
import com.moviereview.review.ReviewStatus;
import com.moviereview.title.Genre;
import com.moviereview.title.GenreRepository;
import com.moviereview.title.MediaType;
import com.moviereview.title.Title;
import com.moviereview.title.TitleMetadata;
import com.moviereview.title.TitleRepository;

@IntegrationTest
class SearchIntegrationTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TitleRepository titleRepository;

	@Autowired
	private GenreRepository genreRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE review, title_genre, title, genre RESTART IDENTITY CASCADE");
		Genre crime = genreRepository.save(new Genre(80, "Crime"));
		Genre drama = genreRepository.save(new Genre(18, "Drama"));

		Title cidadeDeDeus = title(598, MOVIE, "Cidade de Deus", 2002, crime);
		review(cidadeDeDeus, "2019-05-01", 10, "Um clássico sobre crédito, crime e fotografia.", ReviewStatus.PUBLISHED,
				"2026-01-01T00:00:00Z");

		Title dark = title(70523, SERIES, "Dark", 2017, drama);
		review(dark, "2018-01-01", 4, "Confuso demais.", ReviewStatus.PUBLISHED, "2026-02-01T00:00:00Z");
		review(dark, "2023-06-01", 9, "Viagem no tempo em Winden.", ReviewStatus.PUBLISHED, "2026-03-01T00:00:00Z");

		Title clube = title(550, MOVIE, "Clube da Luta", 1999, drama);
		review(clube, "2020-01-01", 6, "Bom, mas datado.", ReviewStatus.PUBLISHED, "2026-01-15T00:00:00Z");
		review(clube, "2021-01-01", 10, "palavrasecreta", ReviewStatus.DRAFT, "2026-04-01T00:00:00Z");

		Title onlyDraft = title(1, MOVIE, "Obra Só Rascunho", 2020, drama);
		review(onlyDraft, "2020-01-01", 8, "Ainda não publicada.", ReviewStatus.DRAFT, "2026-04-01T00:00:00Z");
	}

	@Test
	void listsOnlyTitlesWithPublishedReviewsByRecency() throws Exception {
		mockMvc.perform(get("/api/v1/titles"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalItems").value(3))
			.andExpect(jsonPath("$.items[*].name").value(contains("Dark", "Clube da Luta", "Cidade de Deus")))
			.andExpect(jsonPath("$.items[0].currentRating").value(9))
			.andExpect(jsonPath("$.items[0].sessionCount").value(2));
	}

	@Test
	void textSearchIgnoresAccentsAndCoversNamesAndPublishedContent() throws Exception {
		mockMvc.perform(get("/api/v1/titles?q=credito"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Cidade de Deus")));
		mockMvc.perform(get("/api/v1/titles?q=winden"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Dark")));
		mockMvc.perform(get("/api/v1/titles?q=clube"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Clube da Luta")));
		mockMvc.perform(get("/api/v1/titles?q=palavrasecreta")).andExpect(jsonPath("$.totalItems").value(0));
		mockMvc.perform(get("/api/v1/titles?q=50%_")).andExpect(status().isOk());
	}

	@Test
	void filtersByTypeGenreAndYear() throws Exception {
		mockMvc.perform(get("/api/v1/titles?type=SERIES"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Dark")));
		mockMvc.perform(get("/api/v1/titles?genre=18"))
			.andExpect(jsonPath("$.items[*].name").value(containsInAnyOrder("Dark", "Clube da Luta")));
		mockMvc.perform(get("/api/v1/titles?year=2002"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Cidade de Deus")));
	}

	@Test
	void ratingRangeUsesTheCurrentRating() throws Exception {
		mockMvc.perform(get("/api/v1/titles?minRating=8"))
			.andExpect(jsonPath("$.items[*].name").value(containsInAnyOrder("Dark", "Cidade de Deus")));
		mockMvc.perform(get("/api/v1/titles?maxRating=6"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Clube da Luta")));
		mockMvc.perform(get("/api/v1/titles?minRating=9&maxRating=4")).andExpect(status().isUnprocessableContent());
		mockMvc.perform(get("/api/v1/titles?minRating=0")).andExpect(status().isBadRequest());
	}

	@Test
	void sortsByRatingAndPaginates() throws Exception {
		mockMvc.perform(get("/api/v1/titles?sort=RATING"))
			.andExpect(jsonPath("$.items[*].name").value(contains("Cidade de Deus", "Dark", "Clube da Luta")));
		mockMvc.perform(get("/api/v1/titles?sort=TITLE&size=2&page=1"))
			.andExpect(jsonPath("$.totalItems").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.items[*].name").value(contains("Dark")));
	}

	private Title title(int tmdbId, MediaType mediaType, String name, int year, Genre genre) {
		return titleRepository.save(new Title(new TitleMetadata(tmdbId, mediaType, name, name, (short) year, null,
				null, null, null, null, List.of()), Set.of(genre), name.toLowerCase().replace(' ', '-'), Instant.now()));
	}

	private void review(Title title, String watchedOn, int rating, String content, ReviewStatus status,
			String publishedAt) {
		reviewRepository.save(new Review(title,
				new ReviewContent(LocalDate.parse(watchedOn), rating, content, false, status), TODAY,
				Instant.parse(publishedAt)));
	}

}
