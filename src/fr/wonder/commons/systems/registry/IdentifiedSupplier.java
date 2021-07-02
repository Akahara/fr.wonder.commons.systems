package fr.wonder.commons.systems.registry;

public interface IdentifiedSupplier<T> extends RegistryElement<String> {

	public T get();
	
}
