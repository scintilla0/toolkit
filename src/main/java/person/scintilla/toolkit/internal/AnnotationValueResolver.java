package person.scintilla.toolkit.internal;

import org.springframework.stereotype.Component;

@Component
public class AnnotationValueResolver {

	public static String resolve(String placeholder) {
		if (!placeholder.startsWith("${")) {
			return placeholder;
		}
		String key = placeholder.substring(2, placeholder.length() - 1);
		if ("listEmptyErrorCode".equals(key)) {
			return ToolkitConfigManager.getConfig().getListEmptyErrorCode();
		}
		return null;
	}

}