package fr.wonder.commons.systems.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ProcessOption {
	
	public String name();
	public String shortand() default "null";
	
}
