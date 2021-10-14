package fr.wonder.commons.systems.process.argparser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

final class OptionsClass {
	
	final Map<String, Field> optionFields;
	private final Class<?> clazz;
	private final Constructor<?> constructor;
	
	private OptionsClass(Class<?> clazz, Constructor<?> constructor, Map<String, Field> optionFields) {
		this.clazz = clazz;
		this.constructor = constructor;
		this.optionFields = optionFields;
	}
	
	static OptionsClass createOptionsClass(Class<?> clazz) throws InvalidDeclarationError {
		Constructor<?> constructor;
		try {
			constructor = clazz.getDeclaredConstructor();
			if(!constructor.canAccess(null))
				throw new InvalidDeclarationError("Option class " + clazz.getName() + " does not declare an empty constructor");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new InvalidDeclarationError("Option class " + clazz.getName() + " does not declare an empty constructor", e);
		}
		Map<String, Field> optionFields = new HashMap<>();
		
		for(Field f : clazz.getDeclaredFields()) {
			Option opt = f.getAnnotation(Option.class);
			if(opt == null)
				continue;
			String name = opt.name();
			String shortand = opt.shortand();
			Class<?> type = f.getType();
			if(!ProcessArgumentsHelper.canBeOptionName(name))
				throw new InvalidDeclarationError("Name " + name + " in option class " +
						clazz.getName() + " cannot be an option on field " + f);
			if(optionFields.put(name, f) != null)
				throw new InvalidDeclarationError("Name " + name + " in class " +
						clazz.getName() + " specified twice on field " + f);
			if(!shortand.isEmpty()) {
				if(!ProcessArgumentsHelper.canBeOptionShortand(shortand))
					throw new InvalidDeclarationError("Name " + shortand + " in option class " +
							clazz.getName() + " cannot be a shortand on field " + f);
				if(optionFields.put(shortand, f) != null)
					throw new InvalidDeclarationError("Name " + shortand + " in option class " +
							clazz.getName() + " specified twice");
			}
			if(!ProcessArgumentsHelper.canBeArgumentType(type))
				throw new InvalidDeclarationError("Option of field " + f + " in option class " +
						clazz.getName() + " has invalid type " + type.getName());
		}
		return new OptionsClass(clazz, constructor, optionFields);
	}
	
	Object newInstance() {
		try {
			return constructor.newInstance();
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new IllegalStateException("Cannot instantiace option class " + clazz, e);
		}
	}

	public Collection<String> getAvailableOptionNames() {
		Collection<String> options = new ArrayList<>(optionFields.keySet());
		options.removeIf(opt -> !ProcessArgumentsHelper.canBeOptionName(opt));
		return options;
	}
	
}