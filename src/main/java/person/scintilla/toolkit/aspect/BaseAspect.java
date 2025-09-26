package person.scintilla.toolkit.aspect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @version 0.1.1 2025-09-26
 */
public class BaseAspect {

	private static final Map<String, Logger> loggerMap = new HashMap<>();

	protected Logger fetchLogger(JoinPoint joinPoint) {
		return fetchLogger(joinPoint.getTarget().getClass());
	}

	protected Logger fetchLogger(Method method) {
		return fetchLogger(method.getDeclaringClass());
	}

	protected Logger fetchLogger(Class<?> loggerClass) {
		return loggerMap.computeIfAbsent(loggerClass.getName(), LoggerFactory::getLogger);
	}

	protected Method getMethod(JoinPoint joinPoint) {
		return ((MethodSignature) joinPoint.getSignature()).getMethod();
	}

	protected String getMethodName(JoinPoint joinPoint) {
		return getMethod(joinPoint).getName() + "()";
	}

	protected HttpServletRequest fetchRequest() {
		return (HttpServletRequest) RequestContextHolder.getRequestAttributes().resolveReference(RequestAttributes.REFERENCE_REQUEST);
	}

	protected String getUrlRoot() {
		String url = fetchRequest().getRequestURL().toString();
		return url.substring(0, url.lastIndexOf(fetchRequest().getServletPath()));
	}

	protected String getPureRequestUri() {
		String uri = fetchRequest().getServletPath();
		return uri.contains("?") ? uri.substring(0, uri.indexOf("?")) : uri;
	}

}
