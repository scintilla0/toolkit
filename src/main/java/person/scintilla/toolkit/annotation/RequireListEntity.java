package person.scintilla.toolkit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import person.scintilla.toolkit.validator.ListEntityValidator;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ListEntityValidator.class)
public @interface RequireListEntity {

	@Deprecated
	Class<?>[] groups() default {};
	@Deprecated
	Class<? extends Payload>[] payload() default {};

	String message() default "${listEmptyErrorCode}";

	String name();

	String prefix() default "";

	Class<?> emptyDetector() default RequireListEntity.class;

	boolean require() default true;

	FieldRule[] constraints();

}
