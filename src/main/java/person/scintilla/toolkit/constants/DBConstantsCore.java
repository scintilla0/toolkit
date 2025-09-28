package person.scintilla.toolkit.constants;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dbflute.dbmeta.accessory.DomainEntity;
import org.springframework.util.CollectionUtils;

import person.scintilla.toolkit.internal.ToolkitConfigManager;
import person.scintilla.toolkit.utils.DecimalUtils;
import person.scintilla.toolkit.utils.ReflectiveUtils;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * Requires DecimalUtil, DateTimeUtil.
 * @version 0.2.7 - 2025-09-26
 */
public class DBConstantsCore {

	public static class Common {

		public static final boolean ASC = true;
		public static final boolean DESC = false;

		public static final int NON_PAGE_NUMBER = 1;
		public static final int NON_PAGE_MAX_SIZE = 65535;
		public static final int DELETED_SORT_NUM = 999;

		public static final int REVERSE_CHECKED = -1;
		public static final int UNCHECKED = 0;
		public static final int CHECKED = 1;

		public static final int NOT_DELETED = 0;
		public static final int DELETED = 1;

		public static final int NOT_EDITABLE = 0;
		public static final int EDITABLE = 1;

		public static final String _REVERSE_CHECKED = "-1";
		public static final String _UNCHECKED = "0";
		public static final String _CHECKED = "1";

		public static final String _NOT_DELETED = "0";
		public static final String _DELETED = "1";

		public static final String _NOT_EDITABLE = "0";
		public static final String _EDITABLE = "1";

		public static boolean isChecked(Object checkTargetValue, Object optionValue) {
			if (checkTargetValue instanceof Collection) {
				return ((Collection<?>) checkTargetValue).contains(optionValue) ||
						((Collection<?>) checkTargetValue).stream().anyMatch(value -> DecimalUtils.haveSameValue(value, optionValue));
			} else if (checkTargetValue instanceof String || checkTargetValue instanceof Integer || checkTargetValue instanceof Boolean) {
				return checkTargetValue.equals(optionValue) || DecimalUtils.haveSameValue(checkTargetValue, optionValue);
			}
			return false;
		}

		public static boolean isChecked(Object checkTarget) {
			return DecimalUtils.haveSameValue(CHECKED, checkTarget);
		}

		public static int check(boolean isChecked) {
			return isChecked ? CHECKED : UNCHECKED;
		}

		public static String _check(boolean isChecked) {
			return isChecked ? _CHECKED : _UNCHECKED;
		}

		private static final String DELETE_FLAG_NAME = ToolkitConfigManager.getConfig().getDeleteFlagName();

		public static <Entity extends DomainEntity> Entity deletedEntity(Class<Entity> entityClass) {
			Entity entity = ReflectiveUtils.createInstance(entityClass);
			ReflectiveUtils.setField(entity, DELETE_FLAG_NAME, DELETED);
			return entity;
		}

		public static <Entity extends DomainEntity> Entity _deletedEntity(Class<Entity> entityClass) {
			Entity entity = ReflectiveUtils.createInstance(entityClass);
			ReflectiveUtils.setField(entity, DELETE_FLAG_NAME, _DELETED);
			return entity;
		}

	}

	public static class Option<KeyType> {

		public static String retrieveOptionText(Collection<Object> optionCollection, Map<String, String> optionFieldNameMap, Object fieldValue) {
			String fieldValueResult = StringUtils.wrapBlank(fieldValue);
			if (!CollectionUtils.isEmpty(optionCollection)) {
				for (Map.Entry<String, String> nameEntry : optionFieldNameMap.entrySet()) {
					try {
						Object targetOption = optionCollection.stream().filter(option -> fieldValueResult.equals(StringUtils
								.wrapBlank(ReflectiveUtils.getField(option, nameEntry.getKey(), Object.class)))).findAny().orElse(null);
						if (targetOption != null) {
							return ReflectiveUtils.getField(targetOption, nameEntry.getValue());
						} else {
							return fieldValueResult;
						}
					} catch (RuntimeException exception) {
						if (exception.getCause() instanceof NoSuchFieldException) {
							continue;
						} else {
							throw exception;
						}
					}
				}
			}
			return fieldValueResult;
		}

		public static String retrieveOptionText(Map<?, String> optionMap, Object fieldValue) {
			String fieldValueResult = optionMap.entrySet().stream().filter(entry -> entry.getKey().equals(StringUtils.wrapBlank(fieldValue)) ||
					DecimalUtils.haveSameValue(entry.getKey(), fieldValue)).findAny().map(Map.Entry::getValue).orElse(null);
			return fieldValueResult;
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////

		public static Map<Integer, String> singularOptionMap() {
			return singularOptionMap("");
		}

		public static Map<Integer, String> singularOptionMap(String optionText) {
			return create(Common.CHECKED, optionText).build();
		}

		public static Map<Integer, String> binaryOptionMapA(String option1Text, String option0Text) {
			return create(Common.CHECKED, option1Text).and(Common.UNCHECKED, option0Text).build();
		}

		public static Map<Integer, String> binaryOptionMapB(String option0Text, String option1Text) {
			return create(Common.UNCHECKED, option0Text).and(Common.CHECKED, option1Text).build();
		}

		public static Map<String, String> sequenceOptionMap(int beginNumber, int endNumber, String textFormat) {
			return sequenceOptionMap(beginNumber, endNumber, null, textFormat);
		}

		public static Map<String, String> sequenceOptionMap(int beginNumber, int endNumber, String valueFormat, String textFormat) {
			Option<String> result = null;
			for (int number = beginNumber; number <= endNumber; number ++) {
				String value = DecimalUtils.format(number, StringUtils.ifEmptyThen(valueFormat, "0"));
				String text = DecimalUtils.format(number, textFormat);
				result = (result == null) ? create(value, text) : result.and(value, text);
			}
			return (result == null) ? new Option<String>().build() : result.build();
		}

		private final Map<KeyType, String> optionMap;

		public Option() {
			this.optionMap = new LinkedHashMap<>();
		}

		public static <KeyType> Option<KeyType> create(KeyType key, String value) {
			Option<KeyType> dbOption = new Option<>();
			dbOption.and(key, value);
			return dbOption;
		}

		public Option<KeyType> and(KeyType key, String value) {
			this.optionMap.put(key, value);
			return this;
		}

		public Map<KeyType, String> build() {
			return Collections.unmodifiableMap(this.optionMap);
		}
	}

}
