package person.scintilla.toolkit.internal;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.regex.Pattern;

@Component
public class AnnotationValueResolver {

	public static String resolve(String placeholder) {
		if (!placeholder.startsWith("${")) {
			return placeholder; // 不是占位符，直接返回
		}
		String key = placeholder.substring(2, placeholder.length() - 1);
		if ("listEmptyErrorCode".equals(key)) {
			return ToolkitConfigManager.getConfig().getListEmptyErrorCode();
		}
		return null;
	}

}