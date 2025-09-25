package person.scintilla.toolkit.aspect;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import person.scintilla.toolkit.annotation.ArgumentAndResultCheck;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * @version 0.1.0 2025-09-25
 */
@Aspect
@Component
@Order(10)
public class ArgumentAndResultCheckAspect extends BaseAspect {

	@Pointcut("@annotation(person.scintilla.toolkit.annotation.ArgumentAndResultCheck)")
	public void action() {

	}

	@Around("@annotation(checkConfig)")
	public Object argumentAndResultCheck(ProceedingJoinPoint joinPoint, ArgumentAndResultCheck checkConfig) throws Throwable {
		String part = " condition";
		boolean areAllArgumentsValid = true;
		if (checkConfig.inputCheck()) {
			for (Object parameter : joinPoint.getArgs()) {
				if ((parameter instanceof String && StringUtils.isEmpty((String) parameter)) || parameter == null) {
					areAllArgumentsValid = false;
					break;
				}
			}
		}
		if (areAllArgumentsValid) {
			Object result = joinPoint.proceed();
			part = " result";
			if (void.class.equals(((MethodSignature) joinPoint.getSignature()).getMethod().getReturnType())
					|| !checkConfig.outputCheck() || result != null) {
				return result;
			}
		}
		fetchLogger(joinPoint).info(getMethodName(joinPoint) + part + " empty, access forbidden");
		throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}

	@ControllerAdvice
	private class MethodArgumentTypeMismatchExceptionAdvice {

		@ExceptionHandler(MethodArgumentTypeMismatchException.class)
		public void handling(MethodArgumentTypeMismatchException exception, HttpServletResponse response) throws IOException {
			if (!exception.getMessage().contains("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer';")) {
				throw exception;
			}
			fetchLogger(this.getClass()).info("integer key parse failed, access forbidden");
			response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
		}

	}

}
