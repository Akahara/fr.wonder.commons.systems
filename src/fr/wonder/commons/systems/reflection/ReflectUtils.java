package fr.wonder.commons.systems.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ReflectUtils {
	
	/**
	 * Maps all constants of a class with their names.<br>
	 * This method will collect all {@link #isConstantField(int) constant} fields of the {@code registry} class
	 * that are of type {@code type} (not one of its children) and map them to their name.
	 * If two values conflict an exception will be thrown.
	 * <p>This may be used for info registering:
	 * <blockquote><pre>
	 * public class InfoCodes {
	 *   public static final int SUCCESS = 0,
	 *                           ERROR = -1,
	 *                           ...
	 *   
	 *   public static final Map<Integer, String> infos = mapConstants(int.class, InfoCodes.class);
	 * }
	 * </pre></blockquote>
	 * 
	 * @param <T> the collected types of fields
	 * @param type the explicit type of collected fields
	 * @param registry the class to collect values of
	 * @return a map with values of fields mapped to fields names
	 * @throws IllegalArgumentException if two values of type {@code type} in the class {@code registry}
	 *         are equal.
	 */
	public static <T> Map<T, String> mapConstants(Class<T> type, Class<?> registry) throws IllegalArgumentException {
		Map<T, String> map = new HashMap<>();
		for(Field f : registry.getDeclaredFields()) {
			if(f.getType() == type && isConstantField(f.getModifiers())) {
				try {
					@SuppressWarnings("unchecked")
					T value = (T) f.get(null);
					if(map.containsKey(value))
						throw new IllegalArgumentException("Value " + value + " has two mappings: " + f.getName() + " and " + map.get(value));
					map.put(value, f.getName());
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		return map;
	}
	
	/**
	 * Acts as if <code>getForName(name, registry, false)</code> was called.
	 * 
	 * @param name the name of the field
	 * @param registry the class containing the field
	 * @return the value of the field, or null if no field {@code name} exists in class {@code registry}
	 * @throws IllegalArgumentException if the field exists but is not accessible
	 */
	public static Object getForName(String name, Class<?> registry) throws IllegalArgumentException {
		return getForName(name, registry, false);
	}
	
	/**
	 * Returns the {@code public static} field named {@code name} in class {@code registry}.<br>
	 * For enums this method will return the same value as {@code Enum#valueOf(Class, String)} if
	 * a value named <code>name</code> exists, if not it will return null and won't throw an exception.
	 * 
	 * @param name the name of the field
	 * @param registry the class containing the field
	 * @param ignoreCase if the case of the field should be ignored
	 * @return the value of the field, or null if no field {@code name} exists in class {@code registry}
	 * @throws IllegalArgumentException if the field exists but is not accessible
	 */
	public static Object getForName(String name, Class<?> registry, boolean ignoreCase) throws IllegalArgumentException {
		for(Field f : registry.getDeclaredFields()) {
			if(Modifier.isStatic(f.getModifiers()) && (ignoreCase ? f.getName().equalsIgnoreCase(name) : f.getName().equals(name))) {
				try {
					return f.get(null);
				} catch (IllegalAccessException | ClassCastException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		return null;
	}
	
	/**
	 * Creates a new array of type {@code T} with length {@code length}.<br>
	 * This method behaves exactly like {@link Array#newInstance(Class, int)} but
	 * the returned object is casted immediately to {@code T[]} so that the caller
	 * does not need to suppress the {@code "unchecked"} warning.
	 * 
	 * @param <T> the type of the array
	 * @param type the type of the array
	 * @param length the length of the array
	 * @return a new array of type {@code T} and length {@code length}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] createArray(Class<T> type, int length) {
		return (T[]) Array.newInstance(type, length);
	}
	
	public static Class<?> getComponentType(Class<?> arrayType) {
		while(arrayType.isArray())
			arrayType = arrayType.componentType();
		return arrayType;
	}
	
	public static boolean doModifiersInclude(int modifiers, int required) {
		return (modifiers & required) == required;
	}
	
	public static boolean doModifiersExclude(int modifiers, int excluded) {
		return (~modifiers & excluded) == excluded;
	}
	
	public static boolean doModifiersMatch(int modifiers, int required, int excluded) {
		return ((modifiers & required) == required) && ((~modifiers & excluded) == excluded);
	}
	
	public static boolean isSerializableField(int modifiers) {
		return doModifiersMatch(modifiers, Modifier.PUBLIC, Modifier.TRANSIENT | Modifier.FINAL | Modifier.STATIC);
	}
	
	public static boolean isConstantField(int modifiers) {
		return doModifiersInclude(modifiers, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
	}
	
	// COMMENT many functions from ReflectUtils
	
	public static void runOnClassFields(Class<?> clazz, Consumer<Field> consumer) {
		while(clazz != null) {
			for(Field f : clazz.getDeclaredFields())
				consumer.accept(f);
			clazz = clazz.getSuperclass();
		}
	}
	
	public static List<Field> getClassFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		runOnClassFields(clazz, fields::add);
		return fields;
	}
	
	public static List<Field> getSerializableFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		runOnClassFields(clazz, f -> {
			if(isSerializableField(f.getModifiers()))
				fields.add(f);
		});
		return fields;
	}
	
	public static <R> R accumulateOnClassFields(Class<?> clazz, BiFunction<Field, R, R> consumer, R defaultValue) {
		while(clazz != null && clazz != Enum.class) {
			for(Field f : clazz.getDeclaredFields())
				if((f.getModifiers() & Modifier.STATIC) == 0)
					defaultValue = consumer.apply(f, defaultValue);
			clazz = clazz.getSuperclass();
		}
		return defaultValue;
	}
	
	public static <R> R accumulateOnClassFields(Class<?> clazz, BiFunction<Field, R, R> consumer) {
		return accumulateOnClassFields(clazz, consumer, null);
	}

	public static void runOnInstanceFields(Object obj, Consumer<Object> consumer) {
		Class<?> clazz = obj.getClass();
		runOnClassFields(clazz, f -> {
			try {
				consumer.accept(f.get(obj));
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		});
	}
	
	/**
	 * Returns {@code true} if {@code executable} can be invoked with the given
	 * arguments.
	 * <p>
	 * An executable can be invoked with some arguments if its parameters count is
	 * the same as the arguments' and each one of them matches (order matters).
	 * <p>
	 * Note that primitive types are passed to variadic functions as extended
	 * primitive (an {@code int} becomes an {@code Integer}) so every parameter type
	 * is converted to its extended type if it has one before being compared with
	 * the argument type.
	 * <p>
	 * A {@code null} argument matches any non true primitive parameter.
	 * 
	 * @param executable the executable to test for
	 * @param args       the arguments to test with
	 * @return {@code true} if {@code executable} can be invoked with the given
	 *         arguments
	 * @see PrimitiveUtils#isAssignableFrom(Class, Class)
	 */
	public static boolean argumentsMatch(Executable executable, Object... args) {
		if(args.length != executable.getParameterCount())
			return false;
		Class<?>[] parameters = executable.getParameterTypes();
		for(int i = 0; i < args.length; i++) {
			if(args[i] == null ? parameters[i].isPrimitive() : !PrimitiveUtils.isAssignableFrom(args[i].getClass(), parameters[i]))
				return false;
		}
		return true;
	}

	/**
	 * Creates a new instance of the given class with the given arguments. This
	 * method searches for a valid constructor (a null argument matches any non
	 * primitive parameter) and invokes it if only one has been found. If none or
	 * multiple constructors are found an error is thrown.
	 * 
	 * @param <T>   the class type
	 * @param clazz the class to instantiate
	 * @param args  the arguments passed to the constructor, may contain null
	 *              objects but not be null itself
	 * @return a new instance of the class
	 * @throws IllegalArgumentException if not exactly one constructor has been
	 *                                  found or the constructor invocation throws
	 *                                  an error
	 * @see Constructor#newInstance(Object...) {@link Constructor#newInstance} for
	 *      the errors a constructor invocation can result in
	 * @see #argumentsMatch(Executable, Object...) argumentsMatch
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> clazz, Object... args) throws IllegalArgumentException {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		Constructor<T> matchingConstructor = null;
		for(Constructor<?> c : constructors) {
			if(argumentsMatch(c, args)) {
				if(matchingConstructor != null)
					throw new IllegalArgumentException("Multiple constructor matches given parameters");
				matchingConstructor = (Constructor<T>) c;
			}
		}
		if(matchingConstructor != null) {
			try {
				return matchingConstructor.newInstance(args);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new IllegalArgumentException("Cannot instantiate the class", e);
			}
		}
		throw new IllegalArgumentException("No constructor available for given arguments");
	}

	/**
	 * Creates a new instance of the given class with no arguments.
	 * 
	 * @param <T>   the class type
	 * @param clazz the class to instantiate
	 * @return an instance of the given class
	 * @throws IllegalArgumentException If the class does not declare an empty
	 *                                  constructor or an error occurs while calling
	 *                                  this constructor
	 */
	public static <T> T newInstance(Class<T> clazz) throws IllegalArgumentException {
		try {
			return clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException 
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Cannot instantiate the class", e);
		}
	}

	/**
	 * Used instead of {@link Enum#valueOf(Class, String)} because the {@code Class}
	 * argument's value cannot be casted to an enum class type (we work with {@code Class<?>}
	 * types and we cannot convert to {@code Class<T extends Enum<T>>}), this method
	 * exist solely to avoid suppress warnings annotations in the code.
	 * 
	 * @throws IllegalArgumentException see {@link Enum#valueOf(Class, String)}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object getEnumConstant(Class<?> enumType, String constantName) throws IllegalArgumentException {
		return Enum.valueOf((Class<Enum>) enumType, constantName);
	}

	/**
	 * Returns the class that called the method calling this. Calling this from the
	 * main method will throw an indexOutOfBoundsException.
	 */
	public static Class<?> getCallerClass() {
		return getCallerClass(4);
	}
	
	/**
	 * Returns the class object that takes part in the current 
	 * stack trace. Depending on {@code ignoreCount} a different
	 * class will be returned:
	 * <ul>
	 * <li>0 - returns the Thread class</li>
	 * <li>1 - returns this class (ReflectUtils.class)</li>
	 * <li>2 - returns the class calling this method</li>
	 * </ul>
	 * Values larger than 3 will return classes anterior in the stack
	 * trace. Values that would exceed the stack trace size will throw
	 * an error, this happens when searching for the main method "caller".
	 * 
	 * @throws IndexOutOfBoundsException if {@code ignoreCount} is larger
	 * 			than the stack trace size of the caller + 1.
	 */
	public static Class<?> getCallerClass(int ignoreCount) throws IndexOutOfBoundsException {
		String className = Thread.currentThread().getStackTrace()[ignoreCount].getClassName();
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Unreachable class");
		}
	}

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T createAnnotationInstance(Class<T> annotationType, Map<String, Object> values) {
    	return (T) Proxy.newProxyInstance(annotationType.getClassLoader(),
    			new Class[] { annotationType },
    			new AnnotationInvocationHandler(annotationType, values)
		);
    }
    
}
