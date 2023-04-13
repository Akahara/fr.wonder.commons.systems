package fr.wonder.commons.systems.argparser.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fr.wonder.commons.systems.argparser.ArgParser;

/**
 * A CLI option set, makes the link between CLI options
 * (ie. {@code --verbose} or {@code -f}) and java fields.
 * 
 * <p>
 * Members of Option classes must be marked with {@link Option}
 * to be interpreted as options, to embed an option class
 * in another use {@link InnerOptions}.
 * 
 * <p>
 * Example usage
 * <blockquote><pre>
 * {@literal @}OptionClass
 * public static class Options {
 *   {@literal @}Option(name="--width", shortand="-w")
 *   public int rectWidth;
 *   {@literal @}Option(name="--height", shortand="-h")
 *   public int rectHeight;
 *   {@literal @}Option(name="--color")
 *   public String rectColor;
 * }
 * 
 * {@literal @}EntryPoint(...)
 * public static void entryPoint(Options options) {}
 * </pre></blockquote>
 * 
 * <p>
 * Option classes must have a public constructor that takes no
 * arguments, or no defined constructors at all.
 * 
 * @see ArgParser
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionClass {

}
