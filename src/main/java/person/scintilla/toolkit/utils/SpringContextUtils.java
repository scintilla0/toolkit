package person.scintilla.toolkit.utils;

import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SpringContextUtils implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static volatile ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContextUtils.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		applicationContext = event.getApplicationContext();
		LoggerFactory.getLogger(SpringContextUtils.class).debug("ApplicationContext updated");
	}

	private static ApplicationContext getValidContext() {
		ApplicationContext context = applicationContext;
		if (context == null) {
			throw new IllegalStateException("ApplicationContext not initialized");
		}
		return context;
	}

	public static <BeanType> BeanType getBean(Class<BeanType> beanClass) {
		Objects.requireNonNull(beanClass);
		return getValidContext().getBean(beanClass);
	}

}
