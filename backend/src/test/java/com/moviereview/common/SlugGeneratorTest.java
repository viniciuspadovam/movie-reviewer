package com.moviereview.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

	@Test
	void removesAccentsAndPunctuation() {
		assertThat(SlugGenerator.slugify("Cidade de Deus: O Começo! 2002")).isEqualTo("cidade-de-deus-o-comeco-2002");
	}

	@Test
	void fallsBackWhenNothingIsLeft() {
		assertThat(SlugGenerator.slugify("千と千尋")).isEqualTo("obra");
	}

	@Test
	void appendsSuffixUntilFree() {
		Set<String> taken = Set.of("dark-2017", "dark-2017-2");

		assertThat(SlugGenerator.unique("dark-2017", taken::contains)).isEqualTo("dark-2017-3");
	}

}
