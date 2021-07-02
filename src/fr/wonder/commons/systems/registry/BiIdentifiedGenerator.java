package fr.wonder.commons.systems.registry;

import java.util.function.BiFunction;

import fr.wonder.commons.annotations.Constant;
import fr.wonder.commons.annotations.NonNull;

public interface BiIdentifiedGenerator<T, K, R> extends RegistryElement<String>, BiFunction<T, K, R> {

	@Constant
	@NonNull
	public String getID();

	@NonNull
	public R generate(T t, K k);

	@Override
	public default R apply(T t, K k) {
		return generate(t, k);
	}

}
