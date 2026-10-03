package com.moviereview.search;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.moviereview.common.PageResponse;
import com.moviereview.title.MediaType;

@Repository
class TitleSearchRepository {

	// Only published reviews count: drafts never influence matches, ratings or ordering.
	private static final String BASE_QUERY = """
			WITH published AS (
				SELECT title_id, rating, watched_on, created_at, published_at, search_vector
				FROM review
				WHERE status = 'PUBLISHED'
			), current_rating AS (
				SELECT DISTINCT ON (title_id) title_id, rating AS current_rating
				FROM published
				ORDER BY title_id, watched_on DESC, created_at DESC
			), stats AS (
				SELECT title_id, max(published_at) AS last_published_at, count(*) AS session_count
				FROM published
				GROUP BY title_id
			)
			SELECT t.id, t.slug, t.media_type, t.name, t.release_year, t.poster_path,
				c.current_rating, s.session_count, s.last_published_at, count(*) OVER () AS total
			FROM title t
			JOIN current_rating c ON c.title_id = t.id
			JOIN stats s ON s.title_id = t.id
			WHERE TRUE
			""";

	private static final String QUERY_FILTER = """
			AND (t.search_vector @@ websearch_to_tsquery('portuguese', immutable_unaccent(:query))
				OR immutable_unaccent(t.name) ILIKE '%' || immutable_unaccent(:likeQuery) || '%' ESCAPE '\\'
				OR immutable_unaccent(coalesce(t.original_name, '')) ILIKE '%' || immutable_unaccent(:likeQuery) || '%' ESCAPE '\\'
				OR EXISTS (SELECT 1 FROM published p WHERE p.title_id = t.id
					AND p.search_vector @@ websearch_to_tsquery('portuguese', immutable_unaccent(:query))))
			""";

	private final JdbcClient jdbcClient;

	TitleSearchRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	PageResponse<TitleSearchItem> search(SearchCriteria criteria) {
		StringBuilder sql = new StringBuilder(BASE_QUERY);
		Map<String, Object> params = new HashMap<>();
		if (criteria.hasQuery()) {
			sql.append(QUERY_FILTER);
			params.put("query", criteria.query());
			params.put("likeQuery", escapeLike(criteria.query()));
		}
		if (criteria.mediaType() != null) {
			sql.append(" AND t.media_type = :mediaType");
			params.put("mediaType", criteria.mediaType().name());
		}
		if (criteria.genreId() != null) {
			sql.append(" AND EXISTS (SELECT 1 FROM title_genre tg WHERE tg.title_id = t.id AND tg.genre_id = :genreId)");
			params.put("genreId", criteria.genreId());
		}
		if (criteria.minRating() != null) {
			sql.append(" AND c.current_rating >= :minRating");
			params.put("minRating", criteria.minRating());
		}
		if (criteria.maxRating() != null) {
			sql.append(" AND c.current_rating <= :maxRating");
			params.put("maxRating", criteria.maxRating());
		}
		if (criteria.year() != null) {
			sql.append(" AND t.release_year = :year");
			params.put("year", criteria.year());
		}
		sql.append(" ORDER BY ").append(criteria.sort().orderBy()).append(", t.id LIMIT :limit OFFSET :offset");
		params.put("limit", criteria.size());
		params.put("offset", criteria.page() * criteria.size());

		long[] total = { 0 };
		List<TitleSearchItem> items = jdbcClient.sql(sql.toString()).params(params).query((resultSet, rowNumber) -> {
			total[0] = resultSet.getLong("total");
			return toItem(resultSet);
		}).list();
		int totalPages = (int) Math.ceil((double) total[0] / criteria.size());
		return new PageResponse<>(items, criteria.page(), criteria.size(), total[0], totalPages);
	}

	private static TitleSearchItem toItem(ResultSet resultSet) throws SQLException {
		Integer year = resultSet.getObject("release_year", Integer.class);
		return new TitleSearchItem(resultSet.getLong("id"), resultSet.getString("slug"),
				MediaType.valueOf(resultSet.getString("media_type")), resultSet.getString("name"),
				year == null ? null : year.shortValue(), resultSet.getString("poster_path"),
				resultSet.getInt("current_rating"), resultSet.getInt("session_count"),
				resultSet.getObject("last_published_at", OffsetDateTime.class).toInstant());
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
