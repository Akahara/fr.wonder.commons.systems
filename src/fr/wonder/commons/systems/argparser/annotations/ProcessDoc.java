package fr.wonder.commons.systems.argparser.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to provide CLI programs a meaningful synopsis/help message
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface ProcessDoc {

	/**
	 * The displayed documentation displayed when help is asked for
	 */
	public String doc();
	
}
