package fr.wonder.commons.systems.argparser.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A CLI option.
 *
 * <p>
 * Options can have default values, by simply giving
 * them default values in java:
 * <blockquote>public int option = 2;</blockquote>
 *
 * @see OptionClass
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
	
	/**
	 * The name of the option, does not need to match the
	 * field name, must start with two dashes ({@code --version})
	 */
	public String name();
	/**
	 * An optional shorthand for the option, must start with
	 * a dash and be followed by a single character ({@code -v})
	 */
	public String shorthand() default "";
	/**
	 * When a non-boolean option, gives the value name when help
	 * is displayed ({@code --input <file>} instead of {@code --input <value>})
	 */
	public String valueName() default "value";
	/**
	 * Option info displayed when help is asked for
	 */
	public String desc() default "";
	
}
