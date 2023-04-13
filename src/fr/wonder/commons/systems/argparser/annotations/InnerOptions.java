package fr.wonder.commons.systems.argparser.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a member of an {@link OptionClass} as an options subset.
 * <p>
 * The annotated member must also be an {@code OptionClass}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InnerOptions {

}
