package jedi.db.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
	public String name() default "\\0";
	public String engine() default "InnoDB";
	public String charset() default "utf8";
	public String comment() default "";
}
