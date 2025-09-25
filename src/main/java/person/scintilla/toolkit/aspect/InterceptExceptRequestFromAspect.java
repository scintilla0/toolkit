package person.scintilla.toolkit.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import person.scintilla.toolkit.annotation.RequestFrom;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * @version 0.1.0 2025-9-25
 */
@Aspect
@Component
@Order(20)
public class InterceptExceptRequestFromAspect extends BaseAspect {

	@Pointcut("@annotation(person.scintilla.toolkit.annotation.RequestFrom)")
	public void action() {

	}

	@Before("@annotation(interceptConfig)")
	public void interceptExceptRequestFrom(JoinPoint joinPoint, RequestFrom interceptConfig) {
		String referer = fetchRequest().getHeader("referer"), baseUrl = getUrlRoot();
		for (String fromPath : interceptConfig.path()) {
			if (StringUtils.isAnyEmpty(referer, fromPath)
					|| (!StringUtils.isAnyEmpty(referer, fromPath) && referer.contains(baseUrl + fromPath))) {
				return;
			}
		}
		fetchLogger(joinPoint).info(getMethodName(joinPoint) + " request from a not permitted path, access forbidden");
		throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}

}
