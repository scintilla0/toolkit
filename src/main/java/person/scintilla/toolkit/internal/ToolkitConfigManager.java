package person.scintilla.toolkit.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolkitConfigManager {

	private static ToolkitConfig customConfig;

	public static ToolkitConfig getConfig() {
		return customConfig != null ? customConfig : new DefaultToolkitConfig();
	}

	@Bean
	public ToolkitConfig toolsConfig(ToolkitConfig userConfig) {
		customConfig = userConfig;
		return userConfig;
	}

	@Bean
	@ConditionalOnMissingBean(ToolkitConfig.class)
	public ToolkitConfig defaultToolsConfig() {
		return new DefaultToolkitConfig();
	}

}
