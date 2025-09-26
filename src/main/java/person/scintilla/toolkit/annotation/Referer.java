package person.scintilla.toolkit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import person.scintilla.toolkit.form.BaseForm;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Referer {

	@AliasFor("value")
	String path() default DEFAULT_REFERER;

	@AliasFor("path")
	String value() default DEFAULT_REFERER;

	String DEFAULT_REFERER = "/";

}
