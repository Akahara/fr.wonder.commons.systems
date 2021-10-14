package fr.wonder.commons.systems.process.argparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
	
	public String name();
	public String shortand() default "";
	public String valueName() default "value";
	public String desc() default "";
	
}
