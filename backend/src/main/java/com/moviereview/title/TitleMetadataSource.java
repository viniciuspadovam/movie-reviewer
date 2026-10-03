package com.moviereview.title;

import java.util.List;

public interface TitleMetadataSource {

	List<TitleSearchResult> search(String query, MediaType mediaType);

	TitleMetadata fetch(int tmdbId, MediaType mediaType);

}
