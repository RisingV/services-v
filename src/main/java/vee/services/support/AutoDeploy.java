package vee.services.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA. <br/>
 * Author: Francis Yuen    <br/>
 * Date: 2015-08-10  <br/>
 */
@Target( value = ElementType.TYPE )
@Retention( value = RetentionPolicy.RUNTIME )
public @interface AutoDeploy {

    String name() default "";

    Class<?> type();

}
