package fr.wonder.commons.systems.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Most often than not, a simple {@link HashMap} will be enough, if not this
 * class can be used as a replacement.
 * 
 * <p>
 * A registry is a type of set that holds instances of {@link RegistryElement}
 * with different {@link RegistryElement#getID() IDs}.
 * 
 * <p>
 * A common use of Registry is to maintain a list of {@link IdentifiedGenerator}
 * that can be retrieved by their IDs.
 * 
 * @param <I> The ID type
 * @param <T> The type of {@link RegistryElement}
 */
public class Registry<I, T extends RegistryElement<I>> {

	private final List<T> instances = new ArrayList<>();
	private final boolean acceptNull;

	public Registry() {
		this(true);
	}

	public Registry(boolean acceptNull) {
		this.acceptNull = acceptNull;
	}

	public boolean register(T instance) {
		if (!acceptNull && instance.getID() == null)
			throw new IllegalArgumentException("Cannot register an instance with null id");
		for (T i : instances) {
			if (Objects.equals(i.getID(), instance.getID()))
				return false;
		}
		instances.add(instance);
		return true;
	}
	
	public boolean unregister(I id) {
		for(int i = 0; i < instances.size(); i++) {
			if(Objects.equals(instances.get(i).getID(), id)) {
				instances.remove(i);
				return true;
			}
		}
		return false;
	}

	public T get(I id) {
		for (T i : instances) {
			if (Objects.equals(id, i.getID()))
				return i;
		}
		return null;
	}
	
	public List<T> getItems() {
		return Collections.unmodifiableList(instances);
	}

}
