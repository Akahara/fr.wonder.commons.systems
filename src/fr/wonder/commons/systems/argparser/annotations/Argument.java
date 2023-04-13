package fr.wonder.commons.systems.argparser.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entry point argument, makes the link between CLI
 * arguments and java method parameters.
 */
@Repeatable(Arguments.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {
	
	/**
	 * The argument name, must match the function's parameter's name
	 */
	public String name();
	/**
	 * The argument description, printed when help is asked for
	 */
	public String desc() default "";
	/**
	 * The default value for the argument.
	 * 
	 * <p>When an argument has a default value all the arguments following
	 * it must also have one.
	 * <p>The default value will be interpreted as it would be if the user
	 * typed it in (ie. use "43" to have an int with default value 43).
	 * <p>Default values cannot be empty, even for strings, use options
	 * instead. TODO make emptyable default values
	 */
	public String defaultValue() default "";
	
}
