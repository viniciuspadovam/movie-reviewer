package com.moviereview.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class SlugGenerator {

	private static final int MAX_LENGTH = 300;

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

	private SlugGenerator() {
	}

	public static String slugify(String text) {
		String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		String slug = NON_ALPHANUMERIC.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll("-");
		slug = trimDashes(slug);
		if (slug.length() > MAX_LENGTH) {
			slug = trimDashes(slug.substring(0, MAX_LENGTH));
		}
		return slug.isEmpty() ? "obra" : slug;
	}

	public static String unique(String baseSlug, Predicate<String> isTaken) {
		String candidate = baseSlug;
		int suffix = 2;
		while (isTaken.test(candidate)) {
			candidate = baseSlug + "-" + suffix++;
		}
		return candidate;
	}

	private static String trimDashes(String value) {
		int start = 0;
		int end = value.length();
		while (start < end && value.charAt(start) == '-') {
			start++;
		}
		while (end > start && value.charAt(end - 1) == '-') {
			end--;
		}
		return value.substring(start, end);
	}

}
