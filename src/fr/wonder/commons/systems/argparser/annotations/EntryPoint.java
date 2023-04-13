package fr.wonder.commons.systems.argparser.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.wonder.commons.systems.argparser.ArgParser;

/**
 * A CLI entry point. Makes the link between actual entry points
 * and java methods.
 * 
 * @see ArgParser
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntryPoint {

	public static final String ROOT_ENTRY_POINT = ":root";
	
	/**
	 * Space separated entry point path, or {@link #ROOT_ENTRY_POINT}.
	 */
	public String path();
	/**
	 * Help message displayed when the user does not give valid
	 * arguments or asks for help.
	 */
	public String help() default "";
	
}
