package fr.wonder.commons.systems.registry;

import java.util.function.Function;

import fr.wonder.commons.annotations.Constant;
import fr.wonder.commons.annotations.NonNull;

/**
 * <p>
 * Used to find or generate objects using a single id.
 * 
 * <p>
 * A manager of {@code IdentifiedGenerator} shall maintain a list of {@code T}
 * and when asked to retrieve one shall take an {@code IdentifiedGenerator} as
 * parameter to match its id with every instance in its maintained list.
 * 
 * @param <T> The type of a generated object, it shall have a {@code getId()}
 *            method that returns the same as its generator {@link #getId()}
 *            method.
 * @param <R> The type of an optional data object passed to the
 *            {@link #generate(Object)} method.
 */
public interface IdentifiedGenerator<T, R> extends RegistryElement<String>, Function<T, R> {

	/** Returns the id of the generated object, if it is to be generated */
	@Constant
	@NonNull
	public String getID();

	/**
	 * Generates an an instance of type {@code T} with an id matching
	 * {@link #getId()}
	 */
	@NonNull
	public R generate(T data);

	/**
	 * Delegates to {@link #generate(Object)}, exists for the extension of
	 * {@link Function}, {@code generate} should be preferred if possible.
	 */
	@Override
	public default R apply(T data) {
		return generate(data);
	}

}
