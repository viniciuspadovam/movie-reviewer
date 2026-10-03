package com.moviereview.common;

import java.util.regex.Pattern;

public final class MarkdownExcerpt {

	private static final int DEFAULT_LENGTH = 280;

	private static final Pattern LINK = Pattern.compile("!?\\[([^\\]]*)\\]\\([^)]*\\)");

	private static final Pattern SYNTAX = Pattern.compile("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+\\.)\\s+|[*_`~]");

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private MarkdownExcerpt() {
	}

	public static String of(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return "";
		}
		String text = LINK.matcher(markdown).replaceAll("$1");
		text = SYNTAX.matcher(text).replaceAll("");
		text = WHITESPACE.matcher(text).replaceAll(" ").strip();
		if (text.length() <= DEFAULT_LENGTH) {
			return text;
		}
		int cut = text.lastIndexOf(' ', DEFAULT_LENGTH);
		return text.substring(0, cut > 0 ? cut : DEFAULT_LENGTH) + "…";
	}

}
