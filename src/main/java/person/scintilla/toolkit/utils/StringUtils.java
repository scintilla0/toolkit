package person.scintilla.toolkit.utils;

import java.util.Locale;

public class StringUtils {

	public static final String BLANK = "";
	public static final String SPACE_CHARS = "\\s\\u3000";

	public static String trimSpace(String source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		return source.replaceAll("^[" + SPACE_CHARS + "]+|[" + SPACE_CHARS + "]+$", BLANK);
	}

	public static boolean isEmpty(String source) {
		return source == null || trimSpace(source).isEmpty();
	}

	public static boolean isAnyEmpty(String... sources) {
		for (String source : sources) {
			if (isEmpty(source)) {
				return true;
			}
		}
		return false;
	}

	public static String wrapBlank(Object source) {
		return source == null ? BLANK : source.toString();
	}

	public static String wrapNull(Object source) {
		String result = wrapBlank(source);
		return isEmpty(result) ? null : result;
	}

	public static String ifEmptyThen(String source1, String source2) {
		return isEmpty(source1) ? source2 : source1;
	}

	public static String upperCamelCase(String source) {
		return isEmpty(source) ? source : source.substring(0, 1).toUpperCase(Locale.getDefault()) + source.substring(1);
	}

	public static String connect(String firstSource, String connector, String... sources) {
		StringBuilder builder = new StringBuilder(wrapBlank(firstSource));
		connector = wrapBlank(connector);
		for (String source : sources) {
			if (!isAnyEmpty(builder.toString(), source)) {
				builder.append(connector);
			}
			builder.append(wrapBlank(source));
		}
		return builder.toString();
	}

}
