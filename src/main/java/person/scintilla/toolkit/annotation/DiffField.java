package person.scintilla.toolkit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiffField {

	@AliasFor("value")
	String[] subDiffField() default {};

	@AliasFor("subDiffField")
	String[] value() default {};

}
