package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullBooleanField {
    public boolean required() default true; 
    public boolean unique() default false;
    public String default_value() default "null";
    public String comment() default "";
}
