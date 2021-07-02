package fr.wonder.commons.systems.registry;

import java.util.function.Supplier;

public class IdentifiedSupplierImpl<T> implements IdentifiedSupplier<T> {

	private final String id;
	private final Supplier<T> supplier;
	
	public IdentifiedSupplierImpl(String id, Supplier<T> supplier) {
		this.id = id;
		this.supplier = supplier;
	}

	@Override
	public String getID() {
		return id;
	}
	
	@Override
	public T get() {
		return supplier.get();
	}

}
