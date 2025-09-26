package person.scintilla.toolkit.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import person.scintilla.toolkit.utils.SpringContextUtils;

@Configuration
public class ToolkitBeanAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SpringContextUtils springContextUtils() {
		return new SpringContextUtils();
	}

}