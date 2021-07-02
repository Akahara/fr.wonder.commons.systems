package fr.wonder.commons.systems.registry;

import java.util.function.BiConsumer;

public class ObservableRegistry<I, T extends RegistryElement<I>> extends Registry<I, T> {

	private final BiConsumer<T, Boolean> addEvent;
	private final BiConsumer<I, Boolean> removeEvent;

	public ObservableRegistry(BiConsumer<T, Boolean> addEvent, BiConsumer<I, Boolean> removeEvent) {
		this(addEvent, removeEvent, true);
	}

	public ObservableRegistry(BiConsumer<T, Boolean> addEvent, BiConsumer<I, Boolean> removeEvent, boolean acceptNull) {
		super(acceptNull);
		this.addEvent = addEvent;
		this.removeEvent = removeEvent;
	}

	@Override
	public boolean register(T instance) {
		boolean added = super.register(instance);
		if(addEvent != null)
			addEvent.accept(instance, added);
		return added;
	}
	
	@Override
	public boolean unregister(I id) {
		boolean removed = super.unregister(id);
		if(removeEvent != null)
			removeEvent.accept(id, removed);
		return removed;
	}
}
