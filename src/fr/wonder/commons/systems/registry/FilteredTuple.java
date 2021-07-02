package fr.wonder.commons.systems.registry;

import java.util.Objects;
import java.util.function.Predicate;

public class FilteredTuple<T, K> implements FilteredElement<T, K> {
	
	private final Predicate<T> filter;
	private final K value;
	
	public FilteredTuple(T key, K value) {
		this(t -> Objects.equals(key, t), value);
	}
	
	public FilteredTuple(Predicate<T> keyFilter, K value) {
		this.filter = keyFilter;
		this.value = value;
	}
	
	@Override
	public boolean matches(T key) {
		return filter.test(key);
	}

	@Override
	public K get() {
		return value;
	}

}
