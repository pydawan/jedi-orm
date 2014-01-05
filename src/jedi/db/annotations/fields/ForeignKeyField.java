package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jedi.db.Models;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKeyField {

	public String model();
	
	public String column_name() default "";
	
	public String constraint_name();
	
	public String references();
	
	public String referenced_column() default "";
	
	public String comment() default "";
	
	public Models on_delete() default Models.CASCADE;
	
	public Models on_update() default Models.CASCADE;
}
