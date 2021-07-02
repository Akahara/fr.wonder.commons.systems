package fr.wonder.commons.systems.registry;

import java.util.function.BiFunction;

public class BiIdentifiedGenTuple<T, K, R> implements BiIdentifiedGenerator<T, K, R> {
	
	private final String id;
	private final BiFunction<T, K, R> generator;
	
	public BiIdentifiedGenTuple(String id, BiFunction<T, K, R> generator) {
		this.id = id;
		this.generator = generator;
	}
	
	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public R generate(T t, K k) {
		return generator.apply(t, k);
	}
	
}
