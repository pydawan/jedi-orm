package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {
    public boolean required() default true;
    public int size() default 11;
    public boolean unique() default false;
    public String[] choices() default {};
    public String default_value() default "";
    public String comment() default "";
}