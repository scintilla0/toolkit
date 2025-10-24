package person.scintilla.toolkit.form;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.dbflute.dbmeta.AbstractEntity;
import org.dbflute.dbmeta.DBMeta;
import org.dbflute.dbmeta.info.ColumnInfo;
import org.dbflute.outsidesql.typed.TypedSelectPmb;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import person.scintilla.toolkit.annotation.DiffField;
import person.scintilla.toolkit.annotation.NonSessionField;
import person.scintilla.toolkit.annotation.PMBCondition;
import person.scintilla.toolkit.annotation.PrimaryKeyField;
import person.scintilla.toolkit.annotation.Referer;
import person.scintilla.toolkit.annotation.ResultListField;
import person.scintilla.toolkit.internal.ToolkitConfigManager;
import person.scintilla.toolkit.utils.DateTimeUtils;
import person.scintilla.toolkit.utils.DecimalUtils;
import person.scintilla.toolkit.utils.ReflectiveUtils;
import person.scintilla.toolkit.utils.SpringContextUtils;
import person.scintilla.toolkit.utils.StringUtils;

/**
 * Requires ReflectiveUtils, DecimalUtils, DateTimeUtils.
 * @version 0.1.24 2025-10-23
 */
public class BaseForm implements Serializable {

	private String screenId;
	private String messageKey;
	private String referer = DEFAULT_REFERER;
	private String redirectUrl;
	private String sessionUniqueStamp;
	@NonSessionField
	private transient int initOp = DEFAULT_INIT_OP_INDEX;
	@NonSessionField
	private transient HttpServletRequest _request;
	@NonSessionField
	private transient HttpSession _session;

	/*
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		Z00BaseForm emptyForm = ReflectiveUtil.createInstance(this.getClass());
		fetchAnnotatedFieldStream(NonSessionField.class).forEach(field -> ReflectiveUtil.setField(this, field.getName(),
				ReflectiveUtil.getField(emptyForm, field.getName(), field.getType())));
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		prepareInitField();
	}

	private void prepareInitField() {
		this.initOp = DEFAULT_INIT_OP_INDEX;
	}
	*/

	{
		extractAnnotationReferer();
		doSetDefaultField();
	}

	private void extractAnnotationReferer() {
		Referer refererContainer = AnnotationUtils.findAnnotation(this.getClass(), Referer.class);
		this.setReferer(refererContainer != null ? refererContainer.path() : DEFAULT_REFERER);
	}

	private void doSetDefaultField() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (int index = 0; index < stackTrace.length; index ++) {
			if (stackTrace[index].getClassName().equals(this.getClass().getName())) {
				String nextClassName = stackTrace[index + 1].getClassName();
				if (SET_DEFAULT_FIELD_INITIATOR_CLASS_NAMES.stream().anyMatch(nextClassName::endsWith)) {
					this.setDefaultField();
				}
				break;
			} else {
				continue;
			}
		}
	}

	protected void setDefaultField() {
		// for overriding
	}

	protected <BeanType> BeanType getApplicationBean(Class<BeanType> beanClass) {
		return SpringContextUtils.getBean(beanClass);
	}

	protected HttpServletRequest fetchRequest() {
		return _request != null ? _request : (_request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
	}

	protected HttpSession fetchSession() {
		return _session != null ? _session : (_session = fetchRequest().getSession(true));
	}

	protected <DataType> DataType getDataEntity(Class<DataType> entityClass, String... excludingFields) {
		return createDomainEntity(this, entityClass, excludingFields);
	}

	protected void setDataEntity(Object entity) {
		if (entity == null) {
			return;
		}
		for (Field field : this.getClass().getDeclaredFields()) {
			if (ReflectiveUtils.matchClass(field.getType(), String.class) ||
					ReflectiveUtils.matchClass(field.getType(), Integer.class) ||
					ReflectiveUtils.matchClass(field.getType(), Long.class)) {
				try {
					PropertyDescriptor descriptor = fetchPropertyDescriptor(entity.getClass(), field.getName());
					if (descriptor == null) {
						descriptor = fetchPropertyDescriptorIgnoreCase(entity.getClass(), field.getName());
					}
					if (descriptor != null) {
						Object value = descriptor.getReadMethod().invoke(entity);
						if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), String.class)) {
							value = StringUtils.wrapBlank(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), Integer.class) ||
								ReflectiveUtils.matchClass(descriptor.getPropertyType(), Long.class) ||
								ReflectiveUtils.matchClass(descriptor.getPropertyType(), Double.class) ||
								ReflectiveUtils.matchClass(descriptor.getPropertyType(), BigDecimal.class)) {
							value = DecimalUtils.stringify(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalDate.class)) {
							value = DateTimeUtils.formatDate(value, DEFAULT_DATE_FORMAT);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalTime.class)) {
							value = DateTimeUtils.formatTime_shortColon(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalDateTime.class)) {
							value = DateTimeUtils.format_shortSlashColon(value);
						} else {
							value = "";
						}
						if (value != null) {
							if (ReflectiveUtils.matchClass(field.getType(), String.class)) {
								value = StringUtils.wrapBlank(value);
							} else if (ReflectiveUtils.matchClass(field.getType(), Integer.class)) {
								value = DecimalUtils.toInteger(value);
							} else if (ReflectiveUtils.matchClass(field.getType(), Long.class)) {
								value = DecimalUtils.toLong(value);
							}
							ReflectiveUtils.setField(this, field.getName(), value);
							Field pkField = this.getClass().getDeclaredField(field.getName() + "PK");
							if (pkField != null) {
								ReflectiveUtils.setField(this, pkField.getName(), value);
							}
						}
					}
				} catch (IntrospectionException | IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
					continue;
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Object fetchUniqueAnnotatedFieldValue(Class<? extends Annotation> annotationClass) {
		Class<? extends BaseForm> formClass = this.getClass();
		Map<Class<? extends BaseForm>, Field> fieldMap = UNIQUE_ANNOTATED_FIELD_MAP.computeIfAbsent(annotationClass, key -> new HashMap<>());
		Field formField = fieldMap.computeIfAbsent(formClass, key ->
				Arrays.stream(formClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(annotationClass)).findFirst().orElse(null));
		if (formField == null) {
			return null;
		}
		return ReflectiveUtils.getField(this, formField.getName(), Object.class);
	}

	/**
	 * Use @{@link PrimaryKeyField} to annotate the field that is referred when assess whether an entity is newly created.
	 * @return {@code true} if the annotated primary key is empty.
	 */
	public boolean isNewDetail() {
		Object primaryKey = fetchUniqueAnnotatedFieldValue(PrimaryKeyField.class);
		if (primaryKey == null) {
			return true;
		} else if (ReflectiveUtils.matchType(primaryKey, String.class)) {
			return StringUtils.isEmpty((String) primaryKey);
		}
		return false;
	}

	/**
	 * Use @{@link ResultListField} to annotate the field that is referred when assess whether search result is empty.
	 * @return {@code true} if empty.
	 */
	public boolean isEmptyResult() {
		Object resultContainer = fetchUniqueAnnotatedFieldValue(ResultListField.class);
		if (resultContainer == null) {
			return true;
		} else if (ReflectiveUtils.matchType(resultContainer, Object[].class)) {
			return ((Object[]) resultContainer).length == 0;
		} else if (ReflectiveUtils.matchType(resultContainer, Collection.class)) {
			return ((Collection<?>) resultContainer).isEmpty();
		} else if (ReflectiveUtils.matchType(resultContainer, Map.class)) {
			return ((Map<?, ?>) resultContainer).isEmpty();
		}
		return true;
	}

	private Stream<Field> fetchAnnotatedFieldStream(Class<? extends Annotation> annotationClass) {
		Class<? extends BaseForm> formClass = this.getClass();
		List<Class<?>> classList = new ArrayList<>();
		classList.add(formClass);
		Class<?> superClass = formClass.getSuperclass();
		while (superClass != null && !TOP_SUPER_CLASSES.contains(superClass)) {
			classList.add(superClass);
			superClass = superClass.getSuperclass();
		}
		return classList.stream().map(iterClass -> Arrays.stream(iterClass.getDeclaredFields())
				.filter(field -> annotationClass == null || field.isAnnotationPresent(annotationClass))
				.collect(Collectors.toList())).flatMap(List::stream);
	}

	/**
	 * Use @{@link NonSessionField} to annotate the fields need to be erased when push a form into session.<br>
	 * It should be noticed that acquiring a form from session using {@code getAttribute()} does not trigger initialization processes,
	 * which means those fields need to be prepared while creating should not be cleared away.
	 * @return A new form with same info except the erased fields.
	 */
	public BaseForm generateSessionForm() {
		String[] ignoreFieldNames = fetchAnnotatedFieldStream(NonSessionField.class).map(Field::getName).toArray(String[]::new);
		BaseForm sessionForm = ReflectiveUtils.createInstance(this.getClass());
		BeanUtils.copyProperties(this, sessionForm, ignoreFieldNames);
		return sessionForm;
	}

	/**
	 * Use @{@link DiffField} to annotate the fields to be used in form diff comparing.<br>
	 * When annotating properties using <b>Object</b> or with <b>Object</b> as its generic,
	 * use the value of the annotation to specify which properties of the <b>Object</b> are used for comparison.
	 * <pre><b><i>Eg.:</i></b>&#9;@DiffField("name", "age")
	 * &#9;private User loginUser;</pre>
	 * @param targetForm Target form to be compared.
	 * @return {@code true} if different.
	 */
	public boolean isDifferentTo(BaseForm targetForm) {
		if (targetForm == null || this.getClass() != targetForm.getClass()) {
			return true;
		}
		List<Field> diffFieldList = fetchAnnotatedFieldStream(DiffField.class).collect(Collectors.toList());
		return diffFieldList.stream().anyMatch(field -> haveDifferentContents(
				ReflectiveUtils.getField(this, field.getName(), Object.class),
				ReflectiveUtils.getField(targetForm, field.getName(), Object.class), null, AnnotationUtils.findAnnotation(field, DiffField.class), 0));
	}

	/**
	 * Use @{@link PMBCondition} to annotate the fiels to be set as a query condition when creating a PMB condition.
	 * @param pmbClass Target class of the specified PMB entity.
	 */
	public <Behavior, Entity, PMBType extends TypedSelectPmb<Behavior, Entity>> PMBType generatePMB(Class<PMBType> pmbClass) {
		Objects.requireNonNull(pmbClass);
		PMBType pmb = ReflectiveUtils.createInstance(pmbClass);
		List<String> pmbFieldNameList = Arrays.stream(pmbClass.getSuperclass().getDeclaredFields()).map(Field::getName)
				.filter(fieldName -> fieldName.startsWith("_")).collect(Collectors.toList());
		fetchAnnotatedFieldStream(PMBCondition.class).forEach(formField -> {
			String fieldName = StringUtils.ifEmptyThen(AnnotationUtils.findAnnotation(formField, PMBCondition.class).fieldName(), formField.getName());
			String pmbFieldName = "_" + fieldName;
			Object fieldValue = ReflectiveUtils.getField(this, formField, Object.class);
			if (!pmbFieldNameList.contains(pmbFieldName)) {
				return;
			}
			Field pmbField = ReflectiveUtils.fetchField(pmbClass, pmbFieldName);
			Class<?> pmbFieldType = pmbField.getType(), formFieldType = formField.getType();
			if (ReflectiveUtils.matchClass(pmbFieldType, String.class)) {
				String value = StringUtils.wrapNull(fieldValue);
				if (!pmbFieldNameList.contains(pmbFieldName + "InternalLikeSearchOption")) {
					ReflectiveUtils.setField(pmb, pmbFieldName, value);
				} else {
					try {
						Arrays.stream(Introspector.getBeanInfo(pmbClass).getPropertyDescriptors()).map(PropertyDescriptor::getWriteMethod)
								.filter(method -> method != null && method.getName().matches("^set" + Pattern.quote(StringUtils.upperCamelCase(fieldName)) + "_.+Search$"))
								.forEach(method -> {
									try {
										method.invoke(pmb, value);
									} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
										exception.printStackTrace();
									}
								});
					} catch (IntrospectionException exception) {
						exception.printStackTrace();
					}
				}
			} else if (ReflectiveUtils.matchClass(pmbFieldType, Integer.class)) {
				ReflectiveUtils.setField(pmb, fieldName, DecimalUtils.toInteger(fieldValue));
			} else if (ReflectiveUtils.matchClass(pmbFieldType, LocalDateTime.class)) {
				ReflectiveUtils.setField(pmb, fieldName, DateTimeUtils.parse(fieldValue));
			} else if (ReflectiveUtils.matchClass(pmbFieldType, LocalDate.class)) {
				ReflectiveUtils.setField(pmb, fieldName, DateTimeUtils.parseDate(fieldValue));
			} else if (ReflectiveUtils.matchClass(pmbFieldType, LocalTime.class)) {
				ReflectiveUtils.setField(pmb, fieldName, DateTimeUtils.parseTime(fieldValue));
			} else if (ReflectiveUtils.matchClass(pmbFieldType, List.class)) {
				Type pmbParamType = ((ParameterizedType) pmbField.getGenericType()).getActualTypeArguments()[0];
				if (ReflectiveUtils.matchClass(formFieldType, List.class)) {
					if (CollectionUtils.isEmpty((List<?>) fieldValue)) {
						ReflectiveUtils.setField(pmb, fieldName, null);
					} else {
						Type formParamType = ((ParameterizedType) formField.getGenericType()).getActualTypeArguments()[0];
						if (pmbParamType.equals(formParamType)) {
							ReflectiveUtils.setField(pmb, fieldName, fieldValue);
						} else if (((Class<?>) pmbParamType).equals(Integer.class)) {
							List<Integer> valueList = ((List<?>) fieldValue).stream().map(DecimalUtils::toInteger).collect(Collectors.toList());
							ReflectiveUtils.setField(pmb, fieldName, valueList);
						} else if (((Class<?>) pmbParamType).equals(String.class)) {
							List<String> valueList = ((List<?>) fieldValue).stream().map(StringUtils::wrapNull).collect(Collectors.toList());
							ReflectiveUtils.setField(pmb, fieldName, valueList);
						}
					}
				} else if ((((Class<?>) pmbParamType).equals(Integer.class) && DecimalUtils.parseDecimal(fieldValue) == null) ||
						(((Class<?>) pmbParamType).equals(String.class) && StringUtils.isEmpty(StringUtils.wrapBlank(fieldValue)))) {
					ReflectiveUtils.setField(pmb, fieldName, null);
				} else if (((Class<?>) pmbParamType).equals(Integer.class)) {
					List<Integer> valueList = Collections.singletonList(DecimalUtils.toInteger(fieldValue));
					ReflectiveUtils.setField(pmb, fieldName, valueList);
				} else if (((Class<?>) pmbParamType).equals(String.class)) {
					List<String> valueList = Collections.singletonList(StringUtils.wrapNull(fieldValue));
					ReflectiveUtils.setField(pmb, fieldName, valueList);
				}
			}
		});
		return pmb;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public BindingResult generateBindingResult(String formName, Model model) {
		Objects.requireNonNull(formName);
		Objects.requireNonNull(model);
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(this, formName);
		model.addAttribute("org.springframework.validation.BindingResult." + formName, result);
		return result;
	}

	/**
	 * Create a specified domain entity and copy all field values.
	 * @param targetClass Target class of the specified domain entity.
	 */
	public <TargetType> TargetType createDomainEntity(Class<TargetType> targetClass, String... excludingFields) {
		return createDomainEntity(this, targetClass, excludingFields);
	}

	/**
	 * Extracts <b>MultipartFile</b> content from a multipart request to a form.
	 * @param request Target request to extractFrom.
	 */
	public void extractMultipartFile(MultipartHttpServletRequest request) throws Exception {
		Objects.requireNonNull(request);
		for (Entry<String, List<MultipartFile>> entry : request.getMultiFileMap().entrySet()) {
			for (int fileIndex = 0; fileIndex < entry.getValue().size(); fileIndex ++) {
				MultipartFile file = entry.getValue().get(fileIndex);
				if (file != null) {
					String[] fullFieldName = entry.getKey().split("[.]");
					Object object = this;
					for (String fieldName : fullFieldName) {
						if (fieldName.equals(fullFieldName[fullFieldName.length - 1])) {
							setFileContent(object, fieldName, file, entry.getValue().size(), fileIndex);
						} else {
							if (!fieldName.contains("[")) {
								object = ReflectiveUtils.getField(this, fieldName, Object.class);
							} else {
								int index = Integer.parseInt(fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]")));
								fieldName = fieldName.substring(0, fieldName.indexOf("["));
								Class<?> objectClass = ReflectiveUtils.fetchField(object.getClass(), fieldName).getType();
								Object field = ReflectiveUtils.getField(object, fieldName, Object.class);
								if (objectClass.equals(List.class)) {
									object = ((List<?>) field).get(index);
								} else {
									object = ((Object[]) field)[index];
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Pushes initOpIndex(initial operation index) to session.
	 */
	public void pushInitOpIndexToSession() {
		fetchSession().setAttribute(INIT_OP_INDEX_SESSION_KEY, this.getInitOp());
	}

	/**
	 * Retrieves initOpIndex(initial operation index) from session.
	 */
	public void retrieveInitOpIndexFromSession() {
		Integer initOpIndex = (Integer) fetchSession().getAttribute(INIT_OP_INDEX_SESSION_KEY);
		if (initOpIndex != null) {
			this.setInitOp(initOpIndex);
			fetchSession().removeAttribute(INIT_OP_INDEX_SESSION_KEY);
		}
	}

	/**
	 * Reserve referer from request if in the target scope.
	 */
	public void reserveReferer(String... targetRefererArray) {
		String referer = fetchRequest().getHeader("referer");
		if (!StringUtils.isEmpty(referer)) {
			if (targetRefererArray.length == 0) {
				this.setReferer(referer.substring(referer.indexOf(getUrlRoot()) + getUrlRoot().length()));
			} else {
				for (String targetReferer : targetRefererArray) {
					if (referer.contains(getUrlRoot() + targetReferer)) {
						this.setReferer(referer.substring(referer.indexOf(targetReferer)));
						return;
					}
				}
			}
		}
	}

	/**
	 * Get full referer.
	 */
	public String getFullReferer() {
		return this.getUrlRoot() + this.getReferer();
	}

	private String getUrlRoot() {
		String url = fetchRequest().getRequestURL().toString();
		return url.substring(0, url.lastIndexOf(fetchRequest().getServletPath()));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <TargetType> TargetType createDomainEntity(Object dataEntity, Class<TargetType> targetClass, String... excludingFields) {
		Objects.requireNonNull(dataEntity);
		Objects.requireNonNull(targetClass);
		TargetType entity = ReflectiveUtils.createInstance(targetClass);
		boolean isDBFluteEntity = ReflectiveUtils.matchClass(targetClass, AbstractEntity.class);
		Class<?> superTargetClass = isDBFluteEntity ? targetClass.getSuperclass() : targetClass;
		DBMeta dbMeta = isDBFluteEntity ? ((DBMeta) ReflectiveUtils.invokeMethod(entity, "asDBMeta")) : null;
		List<String> pkFieldList = fetchPkFields(dbMeta, targetClass);
		List<String> commonFieldList = fetchCommonFields(dbMeta);
		for (Field field : (dataEntity instanceof AbstractEntity ? dataEntity.getClass().getSuperclass() : dataEntity.getClass()).getDeclaredFields()) {
			String fieldName = dataEntity instanceof AbstractEntity && field.getName().startsWith("_") ? field.getName().substring(1) : field.getName();
			if ((commonFieldList != null && commonFieldList.contains(fieldName)) || Arrays.asList(excludingFields).contains(fieldName)) {
				continue;
			}
			Object value = ReflectiveUtils.getField(dataEntity, field.getName(), Object.class);
			if (isBasicType(field)) {
				try {
					PropertyDescriptor descriptor = fetchPropertyDescriptor(superTargetClass, fieldName);
					if (descriptor == null) {
						descriptor = fetchPropertyDescriptorIgnoreCase(superTargetClass, fieldName);
					}
					if (descriptor != null) {
						if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), String.class)) {
							value = StringUtils.wrapNull(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), Integer.class)) {
							value = DecimalUtils.toInteger(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), Long.class)) {
							value = DecimalUtils.toLong(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), Double.class)) {
							value = DecimalUtils.toDouble(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), BigDecimal.class)) {
							value = DecimalUtils.parseDecimal(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalDate.class)) {
							value = DateTimeUtils.parseDate(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalTime.class)) {
							value = DateTimeUtils.parseTime(value);
						} else if (ReflectiveUtils.matchClass(descriptor.getPropertyType(), LocalDateTime.class)) {
							value = DateTimeUtils.parse(value);
						} else {
							continue;
						}
						if (pkFieldList != null && pkFieldList.contains(fieldName) && value == null) {
							continue;
						}
						descriptor.getWriteMethod().invoke(entity, value);
					}
				} catch (IntrospectionException | NullPointerException | IllegalAccessException | InvocationTargetException ignored) {
					continue;
				}
			}
		}
		return entity;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static final String DEFAULT_DATE_FORMAT = ToolkitConfigManager.getConfig().getDefaultDateFormat();

	private final String INIT_OP_INDEX_SESSION_KEY = "_init_op_index_" + this.getClass().getName();

	private static final Map<Class<? extends Annotation>, Map<Class<? extends BaseForm>, Field>> UNIQUE_ANNOTATED_FIELD_MAP = new HashMap<>();
	private static final Map<Class<? extends AbstractEntity>, List<String>> PK_FIELDS_MAP = new HashMap<>();
	private static final List<String> SET_DEFAULT_FIELD_INITIATOR_CLASS_NAMES = Collections.unmodifiableList(Arrays.asList("Controller", "Service", "Form"));
	private static final List<Class<?>> TOP_SUPER_CLASSES = Collections.unmodifiableList(Arrays.asList(Object.class, BaseForm.class));
	private static final String DEFAULT_REFERER = Referer.DEFAULT_REFERER;
	private static final int DEFAULT_INIT_OP_INDEX = -1;
	private static final long serialVersionUID = 1L;
	private static List<String> COMMON_FIELDS;

	private static PropertyDescriptor fetchPropertyDescriptor(Class<?> objectClass, String fieldName) throws IntrospectionException {
		return Arrays.stream(Introspector.getBeanInfo(objectClass).getPropertyDescriptors())
				.filter(property -> property.getName().equals(fieldName)).findAny().orElse(null);
	}

	private static PropertyDescriptor fetchPropertyDescriptorIgnoreCase(Class<?> objectClass, String fieldName) throws IntrospectionException {
		return Arrays.stream(Introspector.getBeanInfo(objectClass).getPropertyDescriptors())
				.filter(property -> property.getName().equalsIgnoreCase(fieldName)).findAny().orElse(null);
	}

	private static boolean isBasicType(Field field) {
		Class<?> fieldType = field.getType();
		return ReflectiveUtils.matchClass(fieldType, Integer.class) || ReflectiveUtils.matchClass(fieldType, Long.class) ||
				ReflectiveUtils.matchClass(fieldType, Double.class) || ReflectiveUtils.matchClass(fieldType, Boolean.class) ||
				ReflectiveUtils.matchClass(fieldType, String.class) || ReflectiveUtils.matchClass(fieldType, BigDecimal.class) ||
				ReflectiveUtils.matchClass(fieldType, LocalDate.class) || ReflectiveUtils.matchClass(fieldType, LocalTime.class) ||
				ReflectiveUtils.matchClass(fieldType, LocalDateTime.class);
	}

	private static boolean isBasicType(Object object) {
		return ReflectiveUtils.matchType(object, Integer.class) || ReflectiveUtils.matchType(object, Long.class) ||
				ReflectiveUtils.matchType(object, Double.class) || ReflectiveUtils.matchType(object, Boolean.class) ||
				ReflectiveUtils.matchType(object, String.class) || ReflectiveUtils.matchType(object, BigDecimal.class) ||
				ReflectiveUtils.matchType(object, LocalDate.class) || ReflectiveUtils.matchType(object, LocalTime.class) ||
				ReflectiveUtils.matchType(object, LocalDateTime.class);
	}

	private static <DataType> boolean areBasicEqual(@NonNull DataType object1, @NonNull DataType object2) {
		if (ReflectiveUtils.matchType(object1, BigDecimal.class)) {
			return ((BigDecimal) object1).compareTo((BigDecimal) object2) == 0;
		} else if (ReflectiveUtils.matchType(object1, LocalDateTime.class)) {
			return ((LocalDateTime) object1).isEqual((LocalDateTime) object2);
		} else if (ReflectiveUtils.matchType(object1, LocalDate.class)) {
			return ((LocalDate) object1).isEqual((LocalDate) object2);
		} else if (ReflectiveUtils.matchType(object1, LocalTime.class)) {
			return ((LocalTime) object1).equals((LocalTime) object2);
		}
		return object1.equals(object2);
	}

	private static boolean isObjectEmpty(Object object, Map<Class<?>, Field[]> subDiffFieldMap, DiffField diffFieldAnno, int objectDepth) {
		if (object == null) {
			return true;
		} else if (ReflectiveUtils.matchType(object, String.class)) {
			return StringUtils.isEmpty((String) object);
		} else if (ReflectiveUtils.matchType(object, Object[].class)) {
			Map<Class<?>, Field[]> iterableFieldMap = new HashMap<>();
			return Arrays.stream((Object[]) object).allMatch(element -> isObjectEmpty(element, iterableFieldMap, diffFieldAnno, objectDepth));
		} else if (ReflectiveUtils.matchType(object, Collection.class)) {
			Map<Class<?>, Field[]> iterableFieldMap = new HashMap<>();
			return ((Collection<?>) object).stream().allMatch(element -> isObjectEmpty(element, iterableFieldMap, diffFieldAnno, objectDepth));
		} else if (ReflectiveUtils.matchType(object, Map.class)) {
			Map<Class<?>, Field[]> iterableFieldMap = new HashMap<>();
			return ((Map<?, ?>) object).isEmpty() ||
					((Map<?, ?>) object).entrySet().stream().allMatch(entry -> isObjectEmpty(entry.getValue(), iterableFieldMap, diffFieldAnno, objectDepth));
		} else if (!isBasicType(object)) {
			Class<?> objectClass = object.getClass();
			DiffField recursionDiffFieldAnno = objectDepth == 0 ? diffFieldAnno : null;
			Field[] objectFields = subDiffFieldMap == null ? fetchSubDiffFields(object, objectClass, recursionDiffFieldAnno) :
					subDiffFieldMap.computeIfAbsent(objectClass, key -> fetchSubDiffFields(object, objectClass, recursionDiffFieldAnno));
			int nextObjectDepth = objectDepth + 1;
			return Arrays.stream(objectFields).allMatch(field ->
					isObjectEmpty(ReflectiveUtils.getField(object, field.getName(), Object.class), new HashMap<>(), diffFieldAnno, nextObjectDepth));
		} else {
			return false;
		}
	}

	private static boolean haveDifferentContents(Object object1, Object object2, Map<Class<?>, Field[]> subDiffFieldMap, DiffField diffFieldAnno, int objectDepth) {
		int emptyTestResult = (isObjectEmpty(object1, null, diffFieldAnno, objectDepth) ? 1 : 0) + (isObjectEmpty(object2, null, diffFieldAnno, objectDepth) ? 1 : 0);
		if (emptyTestResult != 0) {
			return emptyTestResult == 1;
		}
		if ((object1.getClass() != object2.getClass()) &&
				!(ReflectiveUtils.matchType(object1, Object[].class) && ReflectiveUtils.matchType(object2, Object[].class)) &&
				!(ReflectiveUtils.matchType(object1, List.class) && ReflectiveUtils.matchType(object2, List.class)) &&
				!(ReflectiveUtils.matchType(object1, Set.class) && ReflectiveUtils.matchType(object2, Set.class)) &&
				!(ReflectiveUtils.matchType(object1, Map.class) && ReflectiveUtils.matchType(object2, Map.class))) {
			return true;
		}
		if (isBasicType(object1)) {
			return !areBasicEqual(object1, object2);
		} else if (ReflectiveUtils.matchType(object1, Object[].class) || ReflectiveUtils.matchType(object1, List.class)) {
			List<?> list1, list2;
			if (ReflectiveUtils.matchType(object1, List.class)) {
				list1 = (List<?>) object1;
				list2 = (List<?>) object2;
			} else {
				list1 = Arrays.asList((Object[]) object1);
				list2 = Arrays.asList((Object[]) object2);
			}
			int smallerSize = Math.min(list1.size(), list2.size()), biggerSize = Math.max(list1.size(), list2.size());
			Map<Class<?>, Field[]> listElementFieldMap = new HashMap<>();
			for (int index = 0; index < smallerSize; index ++) {
				if (haveDifferentContents(list1.get(index), list2.get(index), listElementFieldMap, diffFieldAnno, objectDepth)) {
					return true;
				}
			}
			if (smallerSize != biggerSize) {
				List<?> biggerList = list1.size() > list2.size() ? list1 : list2;
				if (biggerList.stream().skip(smallerSize).anyMatch(element -> !isObjectEmpty(element, listElementFieldMap, diffFieldAnno, objectDepth))) {
					return true;
				}
			}
			return false;
		} else if (ReflectiveUtils.matchType(object1, Set.class)) {
			List<?> copiedSet1 = new ArrayList<>((Set<?>) object1), copiedSet2 = new ArrayList<>((Set<?>) object2);
			Map<Class<?>, Field[]> setElementFieldMap = new HashMap<>();
			for (int i = 0; i < copiedSet1.size();) {
				Object element = copiedSet1.get(i);
				if (element == null) {
					i ++;
					continue;
				}
				Object matchedElement = copiedSet2.stream().filter(target ->
						!haveDifferentContents(element, target, setElementFieldMap, diffFieldAnno, objectDepth)).findAny().orElse(null);
				if (matchedElement == null) {
					return true;
				} else {
					copiedSet1.remove(element);
					copiedSet2.remove(matchedElement);
				}
			}
			return copiedSet1.stream().anyMatch(element -> !isObjectEmpty(element, setElementFieldMap, diffFieldAnno, objectDepth)) ||
					copiedSet2.stream().anyMatch(element -> !isObjectEmpty(element, setElementFieldMap, diffFieldAnno, objectDepth));
		} else if (ReflectiveUtils.matchType(object1, Map.class)) {
			Map<?, ?> map1 = (Map<?, ?>) object1, copiedMap2 = new HashMap<>(((Map<?, ?>) object2));
			Map<Class<?>, Field[]> mapValueFieldMap = new HashMap<>();
			if (map1.entrySet().stream().anyMatch(entry ->
					haveDifferentContents(entry.getValue(), copiedMap2.remove(entry.getKey()), mapValueFieldMap, diffFieldAnno, objectDepth))) {
				return true;
			}
			if (!copiedMap2.isEmpty()) {
				if (copiedMap2.entrySet().stream().anyMatch(entry -> !isObjectEmpty(entry.getValue(), mapValueFieldMap, diffFieldAnno, objectDepth))) {
					return true;
				}
			}
			return false;
		} else {
			Class<?> objectClass = object1.getClass();
			DiffField recursionDiffFieldAnno = objectDepth == 0 ? diffFieldAnno : null;
			Field[] mapValueFields = subDiffFieldMap == null ? fetchSubDiffFields(object1, objectClass, recursionDiffFieldAnno) :
					subDiffFieldMap.computeIfAbsent(objectClass, key -> fetchSubDiffFields(object1, objectClass, recursionDiffFieldAnno));
			int nextObjectDepth = objectDepth + 1;
			return Arrays.stream(mapValueFields).anyMatch(field -> haveDifferentContents(
					ReflectiveUtils.getField(object1, field.getName(), Object.class),
					ReflectiveUtils.getField(object2, field.getName(), Object.class), new HashMap<>(), diffFieldAnno, nextObjectDepth));
		}
	}

	private static Field[] fetchSubDiffFields(Object object, Class<?> objectClass, DiffField diffFieldAnno) {
		List<Class<?>> classList = new ArrayList<>();
		classList.add(objectClass);
		Class<?> superClass = objectClass.getSuperclass();
		while (superClass != null && !TOP_SUPER_CLASSES.contains(superClass)) {
			classList.add(superClass);
			superClass = superClass.getSuperclass();
		}
		return classList.stream().map(iterClass -> {
			Stream<Field> stream = Arrays.stream(iterClass.getDeclaredFields());
			if (diffFieldAnno != null) {
				List<String> subDiffFieldList = Arrays.asList(diffFieldAnno.subDiffField());
				stream = stream.filter(field -> subDiffFieldList.contains(field.getName()));
			}
			return stream.collect(Collectors.toList());
		}).flatMap(List::stream).toArray(Field[]::new);
	}

	private static void setFileContent(Object object, String fieldName, MultipartFile file, int fileArrayLength, int fileIndex) throws ReflectiveOperationException {
		if (!StringUtils.isEmpty(fieldName)) {
			if (!fieldName.contains("[")) {
				try {
					ReflectiveUtils.setField(object, fieldName, file);
				} catch (IllegalArgumentException caught) {
					try {
						@SuppressWarnings("unchecked")
						List<MultipartFile> fileList = ReflectiveUtils.getField(object, fieldName, List.class);
						if (fileList == null) {
							fileList = new ArrayList<>();
						}
						fileList.add(file);
						ReflectiveUtils.setField(object, fieldName, fileList);
					} catch (RuntimeException caughtAgain) {
						MultipartFile[] fileArray = ReflectiveUtils.getField(object, fieldName, MultipartFile[].class);
						if (fileArray == null) {
							fileArray = new MultipartFile[fileArrayLength];
						}
						fileArray[fileIndex] = file;
						ReflectiveUtils.setField(object, fieldName, fileArray);
					}
				}
			} else {
				int index = Integer.parseInt(fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]")));
				fieldName = fieldName.substring(0, fieldName.indexOf("["));
				Class<?> fileFieldClass = ReflectiveUtils.fetchField(object.getClass(), fieldName).getType();
				Object fileContainer = ReflectiveUtils.getField(object, fieldName, Object.class);
				if (fileFieldClass.equals(List.class)) {
					if (fileContainer == null) {
						ReflectiveUtils.setField(object, fieldName, new ArrayList<>());
						fileContainer = ReflectiveUtils.getField(object, fieldName, Object.class);
					}
					@SuppressWarnings("unchecked")
					List<MultipartFile> convertedFileContainer = (List<MultipartFile>) fileContainer;
					while (convertedFileContainer.size() < index - 2) {
						convertedFileContainer.add(null);
					}
					if (convertedFileContainer.size() >= index + 1) {
						convertedFileContainer.set(index, file);
					} else {
						convertedFileContainer.add(file);
					}
				} else {
					if (fileContainer == null) {
						ReflectiveUtils.setField(object, fieldName, new Object[] {new MultipartFile[0]});
						fileContainer = ReflectiveUtils.getField(object, fieldName, Object.class);
					}
					MultipartFile[] convertedFileContainer = (MultipartFile[]) fileContainer;
					if (convertedFileContainer.length <= index) {
						ReflectiveUtils.setField(object, fieldName, new Object[] {Arrays.copyOf(convertedFileContainer, index + 1)});
						fileContainer = ReflectiveUtils.getField(object, fieldName, Object.class);
						convertedFileContainer = (MultipartFile[]) fileContainer;
					}
					convertedFileContainer[index] = file;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> fetchPkFields(DBMeta dbMeta, Class<?> targetClass) {
		return dbMeta == null ? null : PK_FIELDS_MAP.computeIfAbsent((Class<AbstractEntity>) targetClass, key -> dbMeta.hasPrimaryKey() ? dbMeta.getPrimaryInfo()
				.getPrimaryColumnList().stream().map(ColumnInfo::getPropertyName).collect(Collectors.toList()) : Collections.emptyList());
	}

	private static List<String> fetchCommonFields(DBMeta dbMeta) {
		if (dbMeta == null) {
			return null;
		}
		if (COMMON_FIELDS == null) {
			List<String> commonFieldList = dbMeta.getCommonColumnInfoList()
					.stream().map(columnInfo -> columnInfo.getPropertyName()).collect(Collectors.toList());
			commonFieldList.addAll(Arrays.asList("deleteFlag", "delete_flag", "deleteFlg", "delete_flg", "delFlg", "del_flg"));
			COMMON_FIELDS = Collections.unmodifiableList(commonFieldList);
		}
		return COMMON_FIELDS;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getScreenId() {
		return screenId;
	}
	public void setScreenId(String screenId) {
		this.screenId = screenId;
	}
	public String getMessageKey() {
		return messageKey;
	}
	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public String getRedirectUrl() {
		return redirectUrl;
	}
	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
	public String getSessionUniqueStamp() {
		return sessionUniqueStamp;
	}
	public void setSessionUniqueStamp(String sessionUniqueStamp) {
		this.sessionUniqueStamp = sessionUniqueStamp;
	}
	public int getInitOp() {
		return initOp;
	}
	public void setInitOp(int initOp) {
		this.initOp = initOp;
	}

}
