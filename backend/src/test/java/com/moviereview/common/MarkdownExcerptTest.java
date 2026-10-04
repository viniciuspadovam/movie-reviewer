package com.moviereview.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownExcerptTest {

	@Test
	void dropsHeadingsAndMarkdownSyntax() {
		String markdown = """
				## Revisão

				Na segunda vez, a **árvore** fez [sentido](https://example.com).

				> Uma citação
				- um item
				""";

		assertThat(MarkdownExcerpt.of(markdown))
			.isEqualTo("Na segunda vez, a árvore fez sentido. Uma citação um item");
	}

	@Test
	void cutsLongTextAtAWordBoundary() {
		String excerpt = MarkdownExcerpt.of("palavra ".repeat(100));

		assertThat(excerpt).endsWith("palavra…").hasSizeLessThanOrEqualTo(281);
	}

	@Test
	void emptyForBlankContent() {
		assertThat(MarkdownExcerpt.of("  ")).isEmpty();
	}

}
