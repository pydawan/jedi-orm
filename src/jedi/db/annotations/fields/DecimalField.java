package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DecimalField {
	public int scale() default 5;
	public int precision() default 2; 
	public boolean required() default true;
	public boolean unique() default false;
	public String comment() default "";
	public String default_value() default "";
}
