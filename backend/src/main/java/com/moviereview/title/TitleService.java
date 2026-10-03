package com.moviereview.title;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviereview.common.ConflictException;
import com.moviereview.common.NotFoundException;
import com.moviereview.common.SlugGenerator;
import com.moviereview.review.ReviewRepository;
import com.moviereview.title.TitleMetadata.GenreMetadata;

@Service
public class TitleService {

	private static final Logger log = LoggerFactory.getLogger(TitleService.class);

	private final TitleRepository titleRepository;

	private final GenreRepository genreRepository;

	private final ReviewRepository reviewRepository;

	private final TitleMetadataSource metadataSource;

	private final Clock clock;

	TitleService(TitleRepository titleRepository, GenreRepository genreRepository, ReviewRepository reviewRepository,
			TitleMetadataSource metadataSource, Clock clock) {
		this.titleRepository = titleRepository;
		this.genreRepository = genreRepository;
		this.reviewRepository = reviewRepository;
		this.metadataSource = metadataSource;
		this.clock = clock;
	}

	public List<TitleSearchResult> searchExternal(String query, MediaType mediaType) {
		return metadataSource.search(query, mediaType);
	}

	@Transactional
	public ImportResult importTitle(int tmdbId, MediaType mediaType) {
		return titleRepository.findByTmdbIdAndMediaType(tmdbId, mediaType)
			.map(existing -> new ImportResult(TitleResponse.from(existing), false))
			.orElseGet(() -> {
				Title created = create(metadataSource.fetch(tmdbId, mediaType));
				return new ImportResult(TitleResponse.from(created), true);
			});
	}

	@Transactional(readOnly = true)
	public TitleResponse get(long id) {
		return TitleResponse.from(titleRepository.findById(id).orElseThrow(TitleService::notFound));
	}

	@Transactional
	public void delete(long id, boolean cascade) {
		Title title = titleRepository.findById(id).orElseThrow(TitleService::notFound);
		if (!cascade && reviewRepository.existsByTitleId(id)) {
			throw new ConflictException("Esta obra tem reviews. Confirme a exclusão das reviews junto com a obra.");
		}
		titleRepository.delete(title);
		log.info("Obra {} ({}) excluída", title.getId(), title.getName());
	}

	static NotFoundException notFound() {
		return new NotFoundException("Obra não encontrada.");
	}

	private Title create(TitleMetadata metadata) {
		String baseSlug = SlugGenerator.slugify(
				metadata.releaseYear() == null ? metadata.name() : metadata.name() + " " + metadata.releaseYear());
		String slug = SlugGenerator.unique(baseSlug, titleRepository::existsBySlug);
		Title title = titleRepository.save(new Title(metadata, upsertGenres(metadata.genres()), slug, clock.instant()));
		log.info("Obra importada do TMDB: {} (tmdbId={}, tipo={})", title.getName(), title.getTmdbId(),
				title.getMediaType());
		return title;
	}

	private Set<Genre> upsertGenres(List<GenreMetadata> genres) {
		return genres.stream().map(this::upsertGenre).collect(Collectors.toSet());
	}

	private Genre upsertGenre(GenreMetadata metadata) {
		Genre genre = genreRepository.findById(metadata.id())
			.orElseGet(() -> genreRepository.save(new Genre(metadata.id(), metadata.name())));
		genre.rename(metadata.name());
		return genre;
	}

	public record ImportResult(TitleResponse title, boolean created) {
	}

}
