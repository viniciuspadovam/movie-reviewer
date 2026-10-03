package com.moviereview.review;

import static com.moviereview.TestRequests.adminSession;
import static com.moviereview.TestRequests.xsrf;
import static com.moviereview.title.MediaType.MOVIE;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.moviereview.IntegrationTest;
import com.moviereview.title.Genre;
import com.moviereview.title.GenreRepository;
import com.moviereview.title.Title;
import com.moviereview.title.TitleMetadata;
import com.moviereview.title.TitleRepository;

import jakarta.servlet.http.Cookie;

@IntegrationTest
class ReviewIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TitleRepository titleRepository;

	@Autowired
	private GenreRepository genreRepository;

	private Cookie session;

	private Title title;

	@BeforeEach
	void setUp() throws Exception {
		jdbcTemplate.execute("TRUNCATE review, title_genre, title, genre RESTART IDENTITY CASCADE");
		session = adminSession(mockMvc);
		Genre drama = genreRepository.save(new Genre(18, "Drama"));
		title = titleRepository.save(new Title(new TitleMetadata(550, MOVIE,
				"Clube da Luta", "Fight Club", (short) 1999, "...", "/p.jpg", "/b.jpg", (short) 139, null, List.of()),
				Set.of(drama), "clube-da-luta-1999", Instant.now()));
	}

	@Test
	void draftsStayPrivateUntilPublished() throws Exception {
		long id = createReview("2019-03-10", 6, "Primeira vez.", "DRAFT");

		mockMvc.perform(get("/api/v1/reviews/latest")).andExpect(jsonPath("$.totalItems").value(0));
		mockMvc.perform(get("/api/v1/reviews/{id}", id)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/titles/clube-da-luta-1999")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/genres")).andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/v1/admin/reviews?status=DRAFT").cookie(session))
			.andExpect(jsonPath("$.items[0].id").value(id));

		saveReview(put("/api/v1/admin/reviews/{id}", id), "2019-03-10", 6, "Primeira vez.", "PUBLISHED")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.publishedAt").value(notNullValue()));

		mockMvc.perform(get("/api/v1/reviews/latest"))
			.andExpect(jsonPath("$.items[0].id").value(id))
			.andExpect(jsonPath("$.items[0].title.slug").value("clube-da-luta-1999"))
			.andExpect(jsonPath("$.items[0].editedAt").value(nullValue()));
		mockMvc.perform(get("/api/v1/genres")).andExpect(jsonPath("$[0].name").value("Drama"));
	}

	@Test
	void titlePageOrdersSessionsByWatchDateAndShowsRatingEvolution() throws Exception {
		long rewatch = createReview("2024-08-01", 9, "Bem melhor na revisão.", "PUBLISHED");
		long first = createReview("2019-03-10", 6, "Primeira vez.", "PUBLISHED");
		createReview("2025-01-01", 2, "Rascunho que não aparece.", "DRAFT");

		mockMvc.perform(get("/api/v1/titles/clube-da-luta-1999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title.name").value("Clube da Luta"))
			.andExpect(jsonPath("$.currentRating").value(9))
			.andExpect(jsonPath("$.sessions.length()").value(2))
			.andExpect(jsonPath("$.sessions[0].id").value(first))
			.andExpect(jsonPath("$.sessions[0].ratingDelta").value(nullValue()))
			.andExpect(jsonPath("$.sessions[1].id").value(rewatch))
			.andExpect(jsonPath("$.sessions[1].sessionNumber").value(2))
			.andExpect(jsonPath("$.sessions[1].ratingDelta").value(3));

		mockMvc.perform(get("/api/v1/reviews/{id}", rewatch))
			.andExpect(jsonPath("$.sessionNumber").value(2))
			.andExpect(jsonPath("$.content").value("Bem melhor na revisão."))
			.andExpect(jsonPath("$.sessions.length()").value(2));
	}

	@Test
	void editingAPublishedReviewMarksItAsEdited() throws Exception {
		long id = createReview("2019-03-10", 6, "Primeira vez.", "PUBLISHED");

		saveReview(put("/api/v1/admin/reviews/{id}", id), "2019-03-10", 7, "Primeira vez, revisado.", "PUBLISHED")
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/reviews/{id}", id))
			.andExpect(jsonPath("$.rating").value(7))
			.andExpect(jsonPath("$.editedAt").value(notNullValue()));
	}

	@Test
	void rejectsInvalidReviews() throws Exception {
		saveReview(post("/api/v1/admin/titles/{id}/reviews", title.getId()), LocalDate.now().plusDays(2).toString(), 6,
				"x", "DRAFT")
			.andExpect(status().isUnprocessableContent());
		saveReview(post("/api/v1/admin/titles/{id}/reviews", title.getId()), "2019-03-10", 11, "x", "DRAFT")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.rating").value("A nota máxima é 5 estrelas."));
		saveReview(post("/api/v1/admin/titles/{id}/reviews", title.getId()), "2019-03-10", 6, " ", "PUBLISHED")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Uma review publicada precisa ter texto."));
		saveReview(post("/api/v1/admin/titles/{id}/reviews", 999), "2019-03-10", 6, "x", "DRAFT")
			.andExpect(status().isNotFound());
	}

	@Test
	void deletedReviewIsGone() throws Exception {
		long id = createReview("2019-03-10", 6, "Primeira vez.", "PUBLISHED");

		mockMvc.perform(delete("/api/v1/admin/reviews/{id}", id).with(xsrf()).cookie(session))
			.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/admin/reviews/{id}", id).cookie(session)).andExpect(status().isNotFound());
	}

	private long createReview(String watchedOn, int rating, String content, String status) throws Exception {
		String body = saveReview(post("/api/v1/admin/titles/{id}/reviews", title.getId()), watchedOn, rating, content,
				status)
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions saveReview(
			MockHttpServletRequestBuilder request, String watchedOn,
			int rating, String content, String status) throws Exception {
		return mockMvc.perform(request.with(xsrf())
			.cookie(session)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"watchedOn": "%s", "rating": %d, "content": "%s", "hasSpoilers": false, "status": "%s"}"""
				.formatted(watchedOn, rating, content, status)));
	}

}
