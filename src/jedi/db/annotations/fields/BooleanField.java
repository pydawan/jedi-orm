package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanField {
    public boolean required() default true; 
    public boolean unique() default false;
    public boolean default_value() default true;
    public String comment() default "";
}
