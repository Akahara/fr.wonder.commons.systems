package fr.wonder.commons.systems.argparser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fr.wonder.commons.systems.argparser.annotations.InnerOptions;
import fr.wonder.commons.systems.argparser.annotations.Option;
import fr.wonder.commons.systems.argparser.annotations.OptionClass;

final class ProcessOptions {
	
	private final Map<String, Field> optionFields;
	private final Class<?> clazz;
	
	private ProcessOptions(Class<?> clazz, Map<String, Field> optionFields) {
		this.clazz = Objects.requireNonNull(clazz);
		this.optionFields = Objects.requireNonNull(optionFields);
	}
	
	static ProcessOptions createOptionsClass(Class<?> clazz) throws InvalidDeclarationError {
		if(clazz.getAnnotation(OptionClass.class) == null)
			throw new IllegalArgumentException("Class" + clazz + " is not an option class");
		
		getDefaultConstructor(clazz);
		Map<String, Field> optionFields = new HashMap<>();
		collectOptionFields(clazz, optionFields);
		
		return new ProcessOptions(clazz, optionFields);
	}
	
	private static Constructor<?> getDefaultConstructor(Class<?> clazz) throws InvalidDeclarationError {
		try {
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			if(!constructor.canAccess(null))
				throw new InvalidDeclarationError("Option class " + clazz.getName() + " does not declare an empty constructor");
			return constructor;
		} catch (NoSuchMethodException | SecurityException e) {
			throw new InvalidDeclarationError("Option class " + clazz.getName() + " does not declare an empty constructor", e);
		}
	}
	
	private static void collectOptionFields(Class<?> clazz, Map<String, Field> optionFields) throws InvalidDeclarationError {
		if(clazz.getAnnotation(OptionClass.class) == null)
			throw new IllegalArgumentException("Class" + clazz + " is not an option class");
		
		// collect @InnerOptions fields, beware of recursion!
		for(Field f : clazz.getDeclaredFields()) {
			InnerOptions opt = f.getAnnotation(InnerOptions.class);
			if(opt == null) continue;
			Class<?> type = f.getType();
			
			getDefaultConstructor(type); // make sure the type has a default constructor
			collectOptionFields(type, optionFields);
		}
		
		// collect @Option fields
		for(Field f : clazz.getDeclaredFields()) {
			Option opt = f.getAnnotation(Option.class);
			if(opt == null) continue;
			Class<?> type = f.getType();
			String name = opt.name();
			String shortand = opt.shorthand();
			
			if(!ArgParserHelper.canBeArgumentType(type))
				throw new InvalidDeclarationError("Option of field " + f + " in option class " + clazz.getName() + " has invalid type " + type.getName());
			if(!ArgParserHelper.canBeOptionName(name))
				throw new InvalidDeclarationError("Name " + name + " in option class " + clazz.getName() + " cannot be an option on field " + f);
			if(!shortand.isEmpty() && !ArgParserHelper.canBeOptionShortand(shortand))
				throw new InvalidDeclarationError("Name " + shortand + " in option class " + clazz.getName() + " cannot be a shortand on field " + f);
			
			addOptionField(optionFields, name, f);
			if(!shortand.isEmpty())
				addOptionField(optionFields, shortand, f);
		}
	}
	
	private static void addOptionField(Map<String, Field> optionFields, String name, Field field) throws InvalidDeclarationError {
		Field overridenField = optionFields.put(name, field);
		
		if(overridenField != null)
			throw new InvalidDeclarationError("Name '" + name + "' of field '" + field + "' collides with field '" + overridenField + "'");
	}
	
	public Object newInstance() {
		try {
			return createOptionClassInstance(clazz);
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException | InvalidDeclarationError e) {
			throw new IllegalStateException("Cannot instantiace option class " + clazz, e);
		}
	}
	
	/**
	 * Creates a new instance of the given option class, with its inner option class filled
	 * with empty instances.
	 */
	private static Object createOptionClassInstance(Class<?> optionClass)
			throws IllegalAccessException, InvocationTargetException, InstantiationException, InvalidDeclarationError {
		Object instance = getDefaultConstructor(optionClass).newInstance();
		for(Field f : optionClass.getDeclaredFields()) {
			if(f.getType().isAnnotationPresent(OptionClass.class)) {
				f.set(instance, createOptionClassInstance(f.getType()));
			}
		}
		return instance;
	}
	
	public Collection<String> getAvailableOptionNames() {
		Collection<String> options = new ArrayList<>(optionFields.keySet());
		options.removeIf(opt -> !ArgParserHelper.canBeOptionName(opt));
		return options;
	}
	
	public Map<String, Field> getOptionFields() {
		return optionFields;
	}

	/**
	 * When using {@link #newInstance()} a new instance of the OptionClass is created,
	 * if it has OptionClass fields they are initialized. When options values are filled
	 * in the Field objects are used to set values of OptionClass objects, the field itself
	 * does not know which object it corresponds to so this method maps the fields'
	 * types (only for fields of type OptionClass) to the objects instances of their
	 * types in the generated instance.
	 * 
	 * This works because the same OptionClass type cannot be used more than once in
	 * an OptionClass hierarchy, so the whole class hierarchy can be flatten to a map.
	 */
	public Map<Class<?>, Object> getAllOptionFields(Object instance) throws IllegalAccessException {
		if(instance.getClass() != clazz)
			throw new IllegalArgumentException("Expected an instance of " + clazz + ", got an instance of " + instance.getClass());
		Map<Class<?>, Object> fields = new HashMap<>();
		fields.put(clazz, instance);
		collectOptionsFields(instance, fields);
		return fields;
	}
	
	private static void collectOptionsFields(Object instance, Map<Class<?>, Object> fieldsMap) throws IllegalAccessException {
		for(Field f : instance.getClass().getDeclaredFields()) {
			if(f.getType().isAnnotationPresent(OptionClass.class)) {
				Object fieldInstance = f.get(instance);
				fieldsMap.put(f.getType(), fieldInstance);
				collectOptionsFields(fieldInstance, fieldsMap);
			}
		}
	}
	
}