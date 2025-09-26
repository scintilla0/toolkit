package person.scintilla.toolkit.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import person.scintilla.toolkit.utils.DecimalUtils;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * Requires DecimalUtils.
 * @version 0.1.1 2025-09-26
 */
@Aspect
@Component
@Order(0)
public class CommonLogAspect extends BaseAspect {

	@Value("${spring.commonlog.hideurlprimarykey:#{true}}")
	private boolean hideUrlPrimaryKey;

	@SuppressWarnings("unchecked")
	@Around("within(@org.springframework.stereotype.Controller *)")
	public Object commonLog(ProceedingJoinPoint joinPoint) throws Throwable {
		Logger logger = fetchLogger(joinPoint);
		Method method = getMethod(joinPoint);
		String methodName = getMethodName(joinPoint);
		String startLogContent = methodName + " start";
		boolean isRequest, isValidateError = false;
		if (isRequest = method.getAnnotationsByType(RequestMapping.class).length > 0) {
			requestReceivedExtraAction();
			startLogContent += " uri:" + getPureRequestUri();
		} else {
			startLogContent += " [thymeleaf invoke]";
		}
		logger.info(startLogContent);
		String endLogContent = methodName + " end";
		try {
			Object result = joinPoint.proceed();
			if (Arrays.stream(joinPoint.getArgs()).anyMatch(arg -> arg instanceof Errors && ((Errors) arg).hasErrors()) ||
					(result instanceof Map && DecimalUtils.isInScope(((Map<String, ?>) result).get("status"), 1))/* ||
					(result instanceof ApiResponse && !((ApiResponse) result).isSuccess())*/) {
				endLogContent += " [validate error]";
				isValidateError = true;
			}
			if (!isRequest || method.getAnnotationsByType(ResponseBody.class).length > 0) {
				logger.info(endLogContent);
			} else {
				if (result instanceof String) {
					String resultString = StringUtils.wrapBlank(result);
					String resultStringLower = resultString.toLowerCase(Locale.getDefault());
					if (resultStringLower.startsWith("redirect:") || resultStringLower.startsWith("forward:")) {
						if (hideUrlPrimaryKey) {
							StringBuilder resultStringBuilder = new StringBuilder();
							for (String part : resultString.split("/")) {
								resultStringBuilder.append("/").append(DecimalUtils.isLong(part) ? "{}" : part);
							}
							resultString = resultStringBuilder.substring(1);
						}
						logger.info(endLogContent + " " + resultString);
					} else {
						logger.info(endLogContent + " view:" + resultString);
					}
				} else {
					logger.info(endLogContent);
				}
			}
			if (isValidateError) {
				validateErrorExtraAction();
			}
			return result;
		} catch (Throwable throwable) {
			if (throwable instanceof ResponseStatusException) {
				if (HttpStatus.FORBIDDEN.equals(((ResponseStatusException) throwable).getStatus())) {
					logger.info(endLogContent + " [forbidden] view:403");
					accessForbiddenExtraAction();
				}
			}
			throw throwable;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void requestReceivedExtraAction() {

	}

	public void validateErrorExtraAction() {

	}

	public void accessForbiddenExtraAction() {

	}

}
