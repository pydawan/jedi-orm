package jedi.db.annotations.fields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoField {
    public boolean primary_key() default false;
    public boolean required() default false;
    public boolean unique() default false;
    public String db_column() default "";
    public boolean db_index() default false;
    public String db_tablespace() default "";
    public String default_value() default "";
    public boolean editable() default true;
    public String verbose_name() default "";

}
