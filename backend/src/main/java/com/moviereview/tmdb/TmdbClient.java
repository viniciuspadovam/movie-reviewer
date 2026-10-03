package com.moviereview.tmdb;

import java.net.http.HttpClient;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.moviereview.common.ExternalServiceException;
import com.moviereview.common.NotFoundException;
import com.moviereview.title.MediaType;
import com.moviereview.title.TitleMetadata;
import com.moviereview.title.TitleMetadata.GenreMetadata;
import com.moviereview.title.TitleMetadataSource;
import com.moviereview.title.TitleSearchResult;

@Component
class TmdbClient implements TitleMetadataSource {

	private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

	private final RestClient restClient;

	private final TmdbProperties properties;

	TmdbClient(RestClient.Builder restClientBuilder, TmdbProperties properties) {
		this.properties = properties;
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.timeout());
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
			.build();
	}

	@Override
	public List<TitleSearchResult> search(String query, MediaType mediaType) {
		TmdbItem.SearchPage page = call(() -> restClient.get()
			.uri("/search/{path}?query={query}&language={language}&include_adult=false", pathOf(mediaType), query,
					properties.language())
			.retrieve()
			.body(TmdbItem.SearchPage.class));
		if (page == null || page.results() == null) {
			return List.of();
		}
		return page.results().stream().map(item -> toSearchResult(item, mediaType)).toList();
	}

	@Override
	public TitleMetadata fetch(int tmdbId, MediaType mediaType) {
		TmdbItem item = call(() -> restClient.get()
			.uri("/{path}/{id}?language={language}", pathOf(mediaType), tmdbId, properties.language())
			.retrieve()
			.body(TmdbItem.class));
		if (item == null) {
			throw new NotFoundException("Obra não encontrada no TMDB.");
		}
		return toMetadata(item, mediaType);
	}

	private <T> T call(Supplier<T> call) {
		if (!properties.isConfigured()) {
			throw new ExternalServiceException("A integração com o TMDB não está configurada.");
		}
		try {
			return call.get();
		}
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
				throw new NotFoundException("Obra não encontrada no TMDB.");
			}
			log.error("TMDB respondeu com status {}", exception.getStatusCode().value());
			throw new ExternalServiceException("O TMDB não respondeu como esperado. Tente novamente.", exception);
		}
		catch (RestClientException exception) {
			log.error("Falha ao chamar o TMDB", exception);
			throw new ExternalServiceException("Não foi possível falar com o TMDB. Tente novamente.", exception);
		}
	}

	private static String pathOf(MediaType mediaType) {
		return mediaType == MediaType.MOVIE ? "movie" : "tv";
	}

	private static TitleSearchResult toSearchResult(TmdbItem item, MediaType mediaType) {
		return new TitleSearchResult(item.id(), mediaType, item.displayName(), item.displayOriginalName(),
				item.releaseYear(), item.overview(), item.posterPath());
	}

	private static TitleMetadata toMetadata(TmdbItem item, MediaType mediaType) {
		List<GenreMetadata> genres = item.genres() == null ? List.of()
				: item.genres().stream().map(genre -> new GenreMetadata(genre.id(), genre.name())).toList();
		return new TitleMetadata(item.id(), mediaType, item.displayName(), item.displayOriginalName(),
				item.releaseYear(), item.overview(), item.posterPath(), item.backdropPath(),
				toShort(mediaType == MediaType.MOVIE ? item.runtime() : null),
				toShort(mediaType == MediaType.SERIES ? item.numberOfSeasons() : null), genres);
	}

	private static Short toShort(Integer value) {
		return value == null ? null : value.shortValue();
	}

}
