package fr.wonder.commons.systems.registry;

public interface FilteredElement<T, K> {
	
	public boolean matches(T key);
	public K get();
	
}