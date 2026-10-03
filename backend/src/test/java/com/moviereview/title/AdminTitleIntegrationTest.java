package com.moviereview.title;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.moviereview.TestRequests.adminSession;
import static com.moviereview.TestRequests.xsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.moviereview.IntegrationTest;
import com.moviereview.review.Review;
import com.moviereview.review.ReviewContent;
import com.moviereview.review.ReviewRepository;
import com.moviereview.review.ReviewStatus;

import jakarta.servlet.http.Cookie;

@IntegrationTest
class AdminTitleIntegrationTest {

	private static final String FIGHT_CLUB = """
			{"id": 550, "title": "Clube da Luta", "original_title": "Fight Club", "release_date": "1999-10-15",
			 "overview": "Um homem insone...", "poster_path": "/poster.jpg", "backdrop_path": "/backdrop.jpg",
			 "runtime": 139, "genres": [{"id": 18, "name": "Drama"}, {"id": 53, "name": "Thriller"}]}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WireMockServer tmdb;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TitleRepository titleRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	private Cookie session;

	@BeforeEach
	void setUp() throws Exception {
		tmdb.resetAll();
		jdbcTemplate.execute("TRUNCATE review, title_genre, title, genre RESTART IDENTITY CASCADE");
		session = adminSession(mockMvc);
	}

	@Test
	void searchMapsMoviesAndSeriesToOneShape() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/search/tv")).withQueryParam("query", equalTo("dark"))
			.withHeader("Authorization", equalTo("Bearer test-token"))
			.willReturn(okJson("""
					{"results": [{"id": 70523, "name": "Dark", "original_name": "Dark",
					 "first_air_date": "2017-12-01", "overview": "...", "poster_path": "/dark.jpg"}]}""")));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/tmdb/search?q=dark&type=SERIES").cookie(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].tmdbId").value(70523))
			.andExpect(jsonPath("$[0].name").value("Dark"))
			.andExpect(jsonPath("$[0].releaseYear").value(2017))
			.andExpect(jsonPath("$[0].mediaType").value("SERIES"));
	}

	@Test
	void importCreatesTitleOnceAndReturnsExistingAfterwards() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/550")).willReturn(okJson(FIGHT_CLUB)));

		importTitle(550, "MOVIE").andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("clube-da-luta-1999"))
			.andExpect(jsonPath("$.runtimeMinutes").value(139))
			.andExpect(jsonPath("$.genres.length()").value(2))
			.andExpect(jsonPath("$.genres[0].name").value("Drama"));

		importTitle(550, "MOVIE").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void slugCollisionGetsNumericSuffix() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/550")).willReturn(okJson(FIGHT_CLUB)));
		tmdb.stubFor(get(urlPathEqualTo("/movie/551")).willReturn(okJson(FIGHT_CLUB.replace("550", "551"))));

		importTitle(550, "MOVIE").andExpect(status().isCreated());
		importTitle(551, "MOVIE").andExpect(jsonPath("$.slug").value("clube-da-luta-1999-2"));
	}

	@Test
	void unknownTmdbIdIsNotFound() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/999")).willReturn(aResponse().withStatus(404)));

		importTitle(999, "MOVIE").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Obra não encontrada no TMDB."));
	}

	@Test
	void tmdbFailureIsBadGateway() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/550")).willReturn(aResponse().withStatus(500)));

		importTitle(550, "MOVIE").andExpect(status().isBadGateway());
	}

	@Test
	void invalidImportRequestIsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/admin/titles").with(xsrf())
			.cookie(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"mediaType\": \"MOVIE\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.tmdbId").value("Informe o id do TMDB."));
	}

	@Test
	void deletingTitleWithReviewsRequiresCascade() throws Exception {
		tmdb.stubFor(get(urlPathEqualTo("/movie/550")).willReturn(okJson(FIGHT_CLUB)));
		importTitle(550, "MOVIE");
		Title title = titleRepository.findBySlug("clube-da-luta-1999").orElseThrow();
		reviewRepository.save(new Review(title,
				new ReviewContent(LocalDate.of(2020, 1, 1), 8, "", false, ReviewStatus.DRAFT), LocalDate.now(),
				Instant.now()));

		mockMvc.perform(delete("/api/v1/admin/titles/{id}", title.getId()).with(xsrf()).cookie(session))
			.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/admin/titles/{id}?cascade=true", title.getId()).with(xsrf()).cookie(session))
			.andExpect(status().isNoContent());
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/titles/{id}", title.getId()).cookie(session))
			.andExpect(status().isNotFound());
	}

	private ResultActions importTitle(int tmdbId, String mediaType) throws Exception {
		return mockMvc.perform(post("/api/v1/admin/titles").with(xsrf())
			.cookie(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"tmdbId\": %d, \"mediaType\": \"%s\"}".formatted(tmdbId, mediaType)));
	}

}
