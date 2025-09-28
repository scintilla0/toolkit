package person.scintilla.toolkit.form;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.hibernate.validator.internal.engine.messageinterpolation.InterpolationTermType;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenCollector;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenIterator;
import org.springframework.context.MessageSource;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import person.scintilla.toolkit.annotation.NonSessionField;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * @version 0.3.3 - 2025-09-28
 */
public class DialogForm extends BaseForm {

	@NonSessionField
	private int status = 0;

	@NonSessionField
	private Object data;

	@NonSessionField
	private Map<String, String> validateErrors = new LinkedHashMap<String, String>();

	@NonSessionField
	private List<String> informations = new ArrayList<String>();

	public void addValidateErrorCode(String field, String messageCode, String... arrayArgs) {
		addValidateError(field, getMessage(messageCode, arrayArgs));
	}

	public void addValidateErrorCode(String field, String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
		addValidateError(field, getMessage(messageCode, mapArgs, arrayArgs));
	}

	public void addValidateError(String field, String message) {
		Objects.requireNonNull(field);
		if (!message.equals(this.getValidateErrors().get(field))) {
			this.getValidateErrors().put(field, message);
		}
	}

	public void addInformationCode(String messageCode, String... arrayArgs) {
		addInformation(getMessage(messageCode, arrayArgs));
	}

	public void addInformationCode(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
		addInformation(getMessage(messageCode, mapArgs, arrayArgs));
	}

	public void addInformation(String message) {
		if (!this.getInformations().contains(message)) {
			this.getInformations().add(message);
		}
	}

	private String getMessage(String messageCode, String... arrayArgs) {
		return getMessage(messageCode, null, arrayArgs);
	}

	private String getMessage(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
		Objects.requireNonNull(messageCode);
		String message = this.getMessageSource().getMessage(messageCode, null, Locale.getDefault());
		TokenIterator tokenIterator = new TokenIterator(new TokenCollector(message, InterpolationTermType.PARAMETER).getTokenList());
		while (tokenIterator.hasMoreInterpolationTerms()) {
			String term = tokenIterator.nextInterpolationTerm();
			String termName = term.substring(1, term.length() - 1);
			if (!CollectionUtils.isEmpty(mapArgs) && mapArgs.containsKey(termName)) {
				message = message.replace(term, mapArgs.get(termName));
			}
		}
		return MessageFormat.format(message, (Object[]) arrayArgs);
	}

	public void wrapErrorMessage(BindingResult bindingResult) {
		Objects.requireNonNull(bindingResult);
		MessageSource messageSource = getMessageSource();
		if (bindingResult.hasErrors()) {
			for (FieldError error : bindingResult.getFieldErrors()) {
				String message = error.getDefaultMessage();
				if (message == null && error.getCode() != null) {
					message = messageSource.getMessage(error.getCode(), null, Locale.getDefault());
				}
				this.addValidateError(error.getField(), StringUtils.wrapBlank(message));
				this.setStatus(1);
			}
			for (ObjectError error : bindingResult.getGlobalErrors()) {
				String message = error.getDefaultMessage();
				if (message == null && error.getCode() != null) {
					message = messageSource.getMessage(error.getCode(), null, Locale.getDefault());
				}
				this.addValidateError("globalError_" + bindingResult.getGlobalErrors().indexOf(error), StringUtils.wrapBlank(message));
				this.setStatus(1);
			}
		}
	}

	public void autoSettle() {
		autoSettle(null);
	}

	public void autoSettle(Object data) {
		this.setData(data);
		if (!CollectionUtils.isEmpty(this.getInformations())) {
			this.setInformations(null);
		}
		if (!CollectionUtils.isEmpty(this.getValidateErrors())) {
			this.setStatus(1);
		} else {
			if (data == null) {
				this.setStatus(0);
			} else {
				if ((data instanceof Collection && ((Collection<?>) data).isEmpty())
						|| (data instanceof Map && ((Map<?, ?>) data).isEmpty())) {
					this.setStatus(2);
				} else {
					this.setStatus(0);
				}
			}
		}
	}

	public Map<String, Object> response() {
		Map<String, Object> result = new HashMap<>();
		result.put("status", this.getStatus());
		result.put("data", this.getData());
		result.put("validateErrors", this.getValidateErrors());
		result.put("informations", this.getInformations());
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static DialogForm success() {
		return success(null);
	}

	public static DialogForm success(Object data) {
		DialogForm result = status(0);
		result.setData(data);
		return result;
	}

	public static DialogForm error(String message) {
		DialogForm result = status(1);
		if (message != null) {
			result.getValidateErrors().put("globalError_0", message);
		}
		return result;
	}

	public static DialogForm errorCode(String messageCode, String... arrayArgs) {
		DialogForm result = error(null);
		result.getValidateErrors().put("globalError_0", result.getMessage(messageCode, arrayArgs));
		return result;
	}

	public static DialogForm errorCode(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
		DialogForm result = error(null);
		result.getValidateErrors().put("globalError_0", result.getMessage(messageCode, mapArgs, arrayArgs));
		return result;
	}

	public static DialogForm empty(String message) {
		DialogForm result = status(2);
		if (message != null) {
			result.getInformations().add(message);
		}
		return result;
	}

	public static DialogForm emptyCode(String messageCode, String... arrayArgs) {
		DialogForm result = empty(null);
		result.getInformations().add(result.getMessage(messageCode, arrayArgs));
		return result;
	}

	public static DialogForm emptyCode(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
		DialogForm result = empty(null);
		result.getInformations().add(result.getMessage(messageCode, mapArgs, arrayArgs));
		return result;
	}

	public static DialogForm status(int status) {
		DialogForm result = new DialogForm();
		result.setStatus(status);
		return result;
	}

	public static DialogForm getValidateResult(BindingResult bindingResult) {
		Objects.requireNonNull(bindingResult);
		DialogForm result = new DialogForm();
		if (bindingResult.hasErrors()) {
			result.wrapErrorMessage(bindingResult);
		}
		result.autoSettle();
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private MessageSource getMessageSource() {
		return getApplicationBean(MessageSource.class);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public Map<String, String> getValidateErrors() {
		return validateErrors;
	}
	public void setValidateErrors(Map<String, String> validateErrors) {
		this.validateErrors = validateErrors;
	}
	public List<String> getInformations() {
		return informations;
	}
	public void setInformations(List<String> informations) {
		this.informations = informations;
	}

}
