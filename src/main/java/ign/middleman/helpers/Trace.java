package ign.middleman.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: cpatni
 * Date: Oct 24, 2010
 * Time: 1:11:39 AM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trace {
    public static final String NULL = "";
    String metricName() default NULL;
    boolean dispatcher() default false;
    String tracerFactoryName() default NULL;
}