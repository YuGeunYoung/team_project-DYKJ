package com.project.dykj.kis.util;

public final class KisValueUtils {

	private KisValueUtils() {
	}

	public static String firstNonBlank(Object... candidates) {
		if (candidates == null) {
			return null;
		}
		for (Object candidate : candidates) {
			if (candidate == null) {
				continue;
			}
			String text = String.valueOf(candidate).trim();
			if (!text.isEmpty()) {
				return text;
			}
		}
		return null;
	}

	public static Long parseLongOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String cleaned = raw.trim().replaceAll("[^0-9\\-]", "");
		if (cleaned.isBlank() || "-".equals(cleaned)) {
			return null;
		}
		try {
			return Long.parseLong(cleaned);
		} catch (Exception e) {
			return null;
		}
	}

	public static Double parseDoubleOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String cleaned = raw.trim().replaceAll("[^0-9\\-\\.]", "");
		if (cleaned.isBlank() || "-".equals(cleaned)) {
			return null;
		}
		try {
			return Double.parseDouble(cleaned);
		} catch (Exception e) {
			return null;
		}
	}
}
