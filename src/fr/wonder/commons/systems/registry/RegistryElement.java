package fr.wonder.commons.systems.registry;

import fr.wonder.commons.annotations.Constant;

/**
 * An instance usable by {@link Registry}
 * 
 * @param <I> the type of id of this instance
 */
public interface RegistryElement<I> {
	
	@Constant
	public I getID();
	
}
