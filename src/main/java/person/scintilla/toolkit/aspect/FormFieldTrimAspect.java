package person.scintilla.toolkit.aspect;

import java.lang.reflect.Field;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.dbflute.dbmeta.AbstractEntity;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import person.scintilla.toolkit.form.BaseForm;
import person.scintilla.toolkit.utils.StringUtils;
import person.scintilla.toolkit.utils.ReflectiveUtils;

/**
 * Requires Z00BaseForm, ReflectiveUtils.
 * @version 0.1.0 2025-9-25
 */
@Aspect
@Component
@Order(30)
public class FormFieldTrimAspect extends BaseAspect {

	@Before("within(@org.springframework.stereotype.Controller *) && @annotation(org.springframework.web.bind.annotation.RequestMapping)")
	public void trimFormFields(JoinPoint joinPoint) {
		for (Object arg : joinPoint.getArgs()) {
			if (arg instanceof BaseForm && arg != null) {
				for (Field field : arg.getClass().getDeclaredFields()) {
					if (field.getType().equals(String.class)) {
						ReflectiveUtils.setField(arg, field, StringUtils.trimSpace(ReflectiveUtils.getField(arg, field, String.class)));
					} else if (field.getType().equals(List.class)) {
						@SuppressWarnings("unchecked")
						List<Object> list = ReflectiveUtils.getField(arg, field, List.class);
						if (list != null) {
							for (int index = 0; index < list.size(); index ++) {
								Object element = list.get(index);
								if (element == null) {
									continue;
								} else if (element instanceof String) {
									list.set(index, StringUtils.trimSpace((String) element));
								} else if (element instanceof AbstractEntity) {
									for (Field elementField : element.getClass().getSuperclass().getDeclaredFields()) {
										if (elementField.getName().startsWith("_") && elementField.getType().equals(String.class)) {
											ReflectiveUtils.setField(element, elementField.getName(),
													StringUtils.trimSpace(ReflectiveUtils.getField(element, elementField.getName(), String.class)));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
