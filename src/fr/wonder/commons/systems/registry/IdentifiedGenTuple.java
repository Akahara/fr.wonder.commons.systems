package fr.wonder.commons.systems.registry;

import java.util.function.Function;

/**
 * Default implementation of {@link IdentifiedGenerator}, the id is kept
 * as a {@code private final String}.
 */
public class IdentifiedGenTuple<T, R> implements IdentifiedGenerator<T, R> {
	
	private final String id;
	private final Function<T, R> generator;
	
	public IdentifiedGenTuple(String id, Function<T, R> generator) {
		this.id = id;
		this.generator = generator;
	}
	
	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public R generate(T data) {
		return generator.apply(data);
	}
	
}
