package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToManyField {
	
	public String model();
	
	public String column_name() default "";
	
	public String references();
	
	public String referenced_column() default "";	
	
	public String comment() default "";
}
