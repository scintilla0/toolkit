package person.scintilla.toolkit.validator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.messageinterpolation.InterpolationTermType;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenCollector;
import org.hibernate.validator.internal.engine.messageinterpolation.parser.TokenIterator;
import org.springframework.context.MessageSource;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.thymeleaf.util.ArrayUtils;

import person.scintilla.toolkit.annotation.FieldRule;
import person.scintilla.toolkit.annotation.FieldRule.ConstraintType;
import person.scintilla.toolkit.annotation.RequireListEntity;
import person.scintilla.toolkit.internal.AnnotationValueResolver;
import person.scintilla.toolkit.internal.ToolkitConfigManager;
import person.scintilla.toolkit.utils.DateTimeUtils;
import person.scintilla.toolkit.utils.DecimalUtils;
import person.scintilla.toolkit.utils.ReflectiveUtils;
import person.scintilla.toolkit.utils.SpringContextUtils;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * Requires DecimalUtils, DateTimeUtils, ReflectiveUtils.
 * @version 1.0.3 - 2025-09-28
 */
public class ListEntityValidator implements ConstraintValidator<RequireListEntity, List<?>> {

	private RequireListEntity annotation;
	private static final Map<String, Method> CONSTRAINT_METHOD_CACHE = new ConcurrentHashMap<>();
	private static final Map<ConstraintType, Class<?>[]> CONSTRAINT_ARGUMENT_TYPE;
	static {
		Map<ConstraintType, Class<?>[]> constraintArgumentType = new HashMap<>();
		constraintArgumentType.put(ConstraintType.CHECK_TIME, new Class<?>[] {String.class, boolean.class});
		constraintArgumentType.put(ConstraintType.CHECK_DATE, new Class<?>[] {String.class, boolean.class});
		constraintArgumentType.put(ConstraintType.CHECK_DECIMAL, new Class<?>[] {boolean.class, int.class, int.class, boolean.class});
		constraintArgumentType.put(ConstraintType.CHECK_NUMBER, new Class<?>[] {int.class, int.class, boolean.class});
		constraintArgumentType.put(ConstraintType.CHECK_ALPHA_NUM, new Class<?>[] {int.class, int.class});
		constraintArgumentType.put(ConstraintType.CHECK_ALPHA_NUM_PUNC, new Class<?>[] {int.class, int.class});
		constraintArgumentType.put(ConstraintType.CHECK_LENGTH, new Class<?>[] {int.class, int.class});
		constraintArgumentType.put(ConstraintType.CHECK_EMPTY, new Class<?>[] {});
		constraintArgumentType.put(ConstraintType.CHECK_REPEAT, new Class<?>[] {});
		CONSTRAINT_ARGUMENT_TYPE = Collections.unmodifiableMap(constraintArgumentType);
	}

	@Override
	public void initialize(RequireListEntity annotation) {
		this.annotation = annotation;
	}

	@Override
	public boolean isValid(List<?> list, ConstraintValidatorContext context) {
		if (CollectionUtils.isEmpty(list)) {
			if (this.annotation.require()) {
				String messageCode = AnnotationValueResolver.resolve(this.annotation.message());
				String message = new FieldValidator(null).getMessage(messageCode, Collections.singletonMap("name", this.annotation.name()));
				addViolation(context, message);
			}
			return false;
		}
		boolean valid = true;
		Map<String, Set<Object>> repeatSetContainer = new HashMap<>();
		for (int index = 0; index < list.size(); index ++) {
			Object object = list.get(index);
			FieldValidator fieldInfo = new FieldValidator(null, null, index);
			for (FieldRule rule : this.annotation.constraints()) {
				String fieldValue = ReflectiveUtils.getField(object, rule.field());
				fieldInfo.pack(rule.field(), rule.name(), fieldValue);
				String cacheKey = buildCacheKey(rule);
				Class<?>[] argumentTypeArray = CONSTRAINT_ARGUMENT_TYPE.get(rule.constraint());
				Method validateMethod = CONSTRAINT_METHOD_CACHE.computeIfAbsent(cacheKey, key ->
						ReflectiveUtils.fetchMethod(FieldValidator.class, rule.constraint().getConstraintName(), argumentTypeArray));
				Object[] argumentArray;
				if (ConstraintType.CHECK_REPEAT.equals(rule.constraint())) {
					Set<Object> repeatSet = repeatSetContainer.computeIfAbsent(cacheKey, key -> new HashSet<>());
					argumentArray = new Object[] {fieldInfo.getFieldValue(), repeatSet};
				} else {
					argumentArray = buildMethodArgumentArray(rule.field(), rule.params(), argumentTypeArray);
				}
				try {
					validateMethod.invoke(fieldInfo, argumentArray);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
					exception.printStackTrace();
				}
				String message = fieldInfo.listEntityValidatorMessageCache;
				fieldInfo.listEntityValidatorMessageCache = null;
				if (!StringUtils.isEmpty(message)) {
					valid = false;
					addViolation(context, index, rule.field(), message);
				}
			}
		}
		return valid;
	}

	private String buildCacheKey(FieldRule rule) {
		StringBuilder key = new StringBuilder(rule.field()).append(rule.constraint());
		if (!ArrayUtils.isEmpty(rule.params())) {
			Arrays.sort(rule.params());
			for (String param : rule.params()) {
				key.append(";").append(param);
			}
		}
		return key.toString();
	}

	private Object[] buildMethodArgumentArray(String fieldName, String[] params, Class<?>[] argumentTypeArray) {
		if (params.length != argumentTypeArray.length) {
			throw new IllegalArgumentException("Illegal @FieldRule param count. Field: " + fieldName +
					". Expected: " + argumentTypeArray.length + ". Received: " + params.length + ".");
		}
		Object[] argumentArray = new Object[argumentTypeArray.length];
		for (int index = 0; index < params.length; index ++) {
			String param = params[index];
			Class<?> type = argumentTypeArray[index];
			if (StringUtils.isEmpty(param)) {
				throw new IllegalArgumentException("Illegal @FieldRule param. Field: " + fieldName + "(" + index + ")" +
						". Expected: " + type.getSimpleName() + ". Received: Empty.");
			} else if (ReflectiveUtils.matchClass(type, int.class)) {
				try {
					argumentArray[index] = Integer.valueOf(param);
				} catch (NumberFormatException caught) {
					throw new IllegalArgumentException("Illegal @FieldRule param. Field: " + fieldName + "(" + index + ")" +
							". Expected: " + type.getSimpleName() + ". Received: \"" + param + "\".");
				}
			} else if (ReflectiveUtils.matchClass(type, boolean.class)) {
				String paramBoolean = param.toLowerCase();
				if (!paramBoolean.equals("true") && !paramBoolean.equals("false")) {
					throw new IllegalArgumentException("Illegal @FieldRule param. Field: " + fieldName + "(" + index + ")" +
							". Expected: " + type.getSimpleName() + ". Received: \"" + param + "\".");
				}
				argumentArray[index] = Boolean.valueOf(param);
			} else if (ReflectiveUtils.matchClass(type, String.class)) {
				argumentArray[index] = param;
			}
		}
		return argumentArray;
	}

	private void addViolation(ConstraintValidatorContext context, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}

	private void addViolation(ConstraintValidatorContext context, int index, String fieldName, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(fieldName).inIterable().atIndex(index).addConstraintViolation();
	}

	public static class FieldValidator {

		private static final String REPEAT = ToolkitConfigManager.getConfig().getRepeatErrorCode();
		private static final String EMPTY = ToolkitConfigManager.getConfig().getEmptyErrorCode();
		private static final String LENGTH = ToolkitConfigManager.getConfig().getLengthErrorCode();
		private static final String PATTERN_NUMBER = ToolkitConfigManager.getConfig().getPatternNumberErrorCode();
		private static final String PATTERN_ALPHA_NUM = ToolkitConfigManager.getConfig().getPatternAlphaNumErrorCode();
		private static final String PATTERN_ALPHA_NUM_PUNC = ToolkitConfigManager.getConfig().getPatternAlphaNumPuncErrorCode();
		private static final String ALPHA_NUM_REGEX_PATTERN = "^[a-zA-Z0-9]+$";
		private static final String ALPHA_NUM_PUNC_REGEX_PATTERN = "^[a-zA-Z0-9`!@#$%^&*()-=~_+\\[\\]\\{}|;':\",./<>?]+$";

		////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private BindingResult result;
		private String lineNumber;
		private String fieldValue;
		private String fieldName;
		private String fieldNamePrefix;
		private String displayName;
		private String displayNamePrefix;
		private String errorMessage;
		private String listEntityValidatorMessageCache;
		private boolean againstValue = true;
		private boolean usePrefix = true;
		private boolean usedForList = true;

		private MessageSource messageSource = getMessageSource();

		public FieldValidator(BindingResult result) {
			this(result, null, -1);
		}

		public FieldValidator(BindingResult result, String resultListName, int index) {
			this.result = result;
			this.lineNumber = (index + 1) + "";
			this.fieldNamePrefix = StringUtils.wrapBlank(resultListName) + "[" + index + "].";
			this.displayNamePrefix = getMessage("E000detailPrefix", this.lineNumber);
			if (index < 0) {
				this.usePrefix = false;
				this.usedForList = false;
			}
		}

		public FieldValidator pack(String fieldName, String name) {
			pack(fieldName, name, null);
			return this;
		}

		public FieldValidator pack(String fieldName, String name, String fieldValue) {
			this.fieldValue = StringUtils.wrapBlank(fieldValue);
			this.fieldName = StringUtils.wrapBlank(fieldName);
			this.displayName = StringUtils.wrapBlank(name);
			this.errorMessage = null;
			return this;
		}

		public FieldValidator message(String messageCode, String... arrayArgs) {
			this.errorMessage = getMessage(messageCode, arrayArgs);
			return this;
		}

		public FieldValidator message(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
			this.errorMessage = getMessage(messageCode, mapArgs, arrayArgs);
			return this;
		}

		public FieldValidator clearMessage() {
			this.errorMessage = null;
			return this;
		}

		public FieldValidator usePrefix() {
			return usePrefix(true);
		}

		public FieldValidator unusePrefix() {
			return usePrefix(false);
		}

		private FieldValidator usePrefix(boolean usePrefix) {
			this.usePrefix = usePrefix;
			return this;
		}

		public FieldValidator enableAgainstValue() {
			return againstValue(true);
		}

		public FieldValidator disableAgainstValue() {
			return againstValue(false);
		}

		public FieldValidator againstValue(boolean rejectValue) {
			this.againstValue = rejectValue;
			return this;
		}

		public String getFieldValue() {
			return this.fieldValue;
		}

		public String getFieldName() {
			return this.fieldName;
		}

		public String getFieldNamePrefix() {
			return this.fieldNamePrefix;
		}

		public String getFieldNameFull() {
			return this.usedForList ? StringUtils.connect(this.fieldNamePrefix, null, this.fieldName) : this.fieldName;
		}

		public String getName() {
			return this.displayName;
		}

		public String getNamePrefix() {
			return this.displayNamePrefix;
		}

		public String getNameFull() {
			return this.usedForList ? StringUtils.connect(this.displayNamePrefix, null, this.displayName) : this.displayName;
		}

		public String getLineNumber() {
			return this.lineNumber;
		}

		public void reject(String messageCode, String... arrayArgs) {
			rejectField(getMessage(messageCode, arrayArgs));
		}

		public void reject(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
			rejectField(getMessage(messageCode, mapArgs, arrayArgs));
		}

		public String getMessage(String messageCode, String... arrayArgs) {
			return getMessage(messageCode, null, arrayArgs);
		}

		public String getMessage(String messageCode, Map<String, String> mapArgs, String... arrayArgs) {
			String message = this.messageSource.getMessage(messageCode, null, Locale.getDefault());
			TokenIterator tokenIterator = new TokenIterator(new TokenCollector(message, InterpolationTermType.PARAMETER).getTokenList());
			while (tokenIterator.hasMoreInterpolationTerms()) {
				String term = tokenIterator.nextInterpolationTerm();
				String termName = term.substring(1, term.length() - 1);
				if (!CollectionUtils.isEmpty(mapArgs) && mapArgs.containsKey(termName)) {
					message = message.replace(term, mapArgs.get(termName));
				} else if ("name".equals(termName)) {
					message = message.replace(term, this.getName());
				}
			}
			return MessageFormat.format(message, (Object[]) arrayArgs);
		}

		public boolean hasFieldErrors() {
			return this.result != null ? this.result.hasFieldErrors(getFieldNameFull()) : false;
		}

		private void rejectField(String message) {
			if (this.errorMessage != null) {
				message = this.errorMessage;
			}
			message = (this.usePrefix ? getNamePrefix() : "") + message;
			if (this.result != null) {
				if (this.againstValue) {
					this.result.rejectValue(getFieldNameFull(), null, message);
				} else {
					this.result.reject(null, message);
				}
			} else {
				this.listEntityValidatorMessageCache = message;
			}
		}

		public FieldValidator checkTime(String format, boolean isRequired) {
			if (!hasFieldErrors()) {
				if (isRequired && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) && !DateTimeUtils.isTime(getFieldValue(), format)) {
					reject(PATTERN_NUMBER);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkDate(String format, boolean isRequired) {
			if (!hasFieldErrors()) {
				if (isRequired && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) && !DateTimeUtils.isDate(getFieldValue(), format)) {
					reject(PATTERN_NUMBER);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkDecimal(boolean isRequired, int maxIntegralLength, int maxFractionalLength, boolean allowMinus) {
			if (!hasFieldErrors()) {
				if (isRequired && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) &&
						(!DecimalUtils.isDecimal(getFieldValue()) || (!allowMinus && DecimalUtils.isAscendingNotEqual(getFieldValue(), 0)))) {
					reject(PATTERN_NUMBER);
				} else if (!StringUtils.isEmpty(getFieldValue()) &&
						(DecimalUtils.getIntegralLength(getFieldValue()) > maxIntegralLength ||
								DecimalUtils.getFractionalLength(getFieldValue()) > maxFractionalLength)) {
					reject(LENGTH);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkNumber(int minLength, int maxLength, boolean allowMinus) {
			if (!hasFieldErrors()) {
				if (minLength != 0 && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) && (!DecimalUtils.isLong(getFieldValue()) ||
						(!allowMinus && DecimalUtils.isAscendingNotEqual(getFieldValue(), 0)))) {
					reject(PATTERN_NUMBER);
				} else if (!StringUtils.isEmpty(getFieldValue()) &&
						!DecimalUtils.isAscending(minLength, DecimalUtils.getIntegralLength(getFieldValue()), maxLength)) {
					reject(LENGTH);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkAlphaNum(int minLength, int maxLength) {
			return checkAlphaNumPunc(minLength, maxLength, ALPHA_NUM_REGEX_PATTERN, PATTERN_ALPHA_NUM);
		}

		public FieldValidator checkAlphaNumPunc(int minLength, int maxLength) {
			return checkAlphaNumPunc(minLength, maxLength, ALPHA_NUM_PUNC_REGEX_PATTERN, PATTERN_ALPHA_NUM_PUNC);
		}

		private FieldValidator checkAlphaNumPunc(int minLength, int maxLength, String regexPattern, String errorMessageCode) {
			if (!hasFieldErrors()) {
				if (minLength != 0 && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) && !getFieldValue().matches(regexPattern)) {
					reject(errorMessageCode);
				} else if (!StringUtils.isEmpty(getFieldValue()) && !DecimalUtils.isAscending(minLength, getFieldValue().length(), maxLength)) {
					reject(LENGTH);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkLength(int minLength, int maxLength) {
			if (!hasFieldErrors()) {
				if (minLength != 0 && StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				} else if (!StringUtils.isEmpty(getFieldValue()) && !DecimalUtils.isAscending(minLength, getFieldValue().length(), maxLength)) {
					reject(LENGTH);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkEmpty() {
			if (!hasFieldErrors()) {
				if (StringUtils.isEmpty(getFieldValue())) {
					reject(EMPTY);
				}
			}
			return this.clearMessage();
		}

		public FieldValidator checkEmpty(Integer value) {
			if (!hasFieldErrors()) {
				if (value == null) {
					reject(EMPTY);
				}
			}
			return this.clearMessage();
		}

		public <Type> FieldValidator checkRepeat(Type value, Set<Type> checkRepeatSet) {
			if (!hasFieldErrors()) {
				if (checkRepeatSet != null && !(value == null || (value instanceof String && StringUtils.isEmpty((String) value)))) {
					if (checkRepeatSet.stream().anyMatch(target -> target.equals(value))) {
						reject(REPEAT);
					}
					checkRepeatSet.add(value);
				}
			}
			return this.clearMessage();
		}

		private MessageSource getMessageSource() {
			return SpringContextUtils.getBean(MessageSource.class);
		}

	}

}
