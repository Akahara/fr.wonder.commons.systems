package fr.wonder.commons.systems.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import fr.wonder.commons.exceptions.UnreachableException;

public class PrimitiveUtils {
	
	/**
	 * Exhaustive list of all <i>true</i> primitive types, namely <code>int.class, float.class...</code><br>
	 * These are the same as <code>Integer.TYPE, Float.TYPE...</code>
	 */
	public static final Class<?>[] TRUE_PRIMITIVE_TYPES =     { int.class, float.class, long.class, double.class, byte.class, short.class, char.class, boolean.class };
	/**
	 * Exhaustive list of all object-wrapped primitive types, namely <code>Integer.class, Float.class...</code>
	 */
	public static final Class<?>[] EXTENDED_PRIMITIVE_TYPES = { Integer.class, Float.class, Long.class, Double.class, Byte.class, Short.class, Character.class, Boolean.class };
	/**
	 * List of all primitive types, this list contains all {@link #TRUE_PRIMITIVE_TYPES} and {@link #EXTENDED_PRIMITIVE_TYPES},
	 * interlaced for the extended types to immediately follow their respective true primitive type.
	 */
	public static final Class<?>[] PRIMITIVE_TYPES;
	
	static {
		PRIMITIVE_TYPES = new Class<?>[2*TRUE_PRIMITIVE_TYPES.length];
		for(int i = 0; i < TRUE_PRIMITIVE_TYPES.length; i++) {
			PRIMITIVE_TYPES[i*2]   = TRUE_PRIMITIVE_TYPES[i];
			PRIMITIVE_TYPES[i*2+1] = EXTENDED_PRIMITIVE_TYPES[i];
		}
	}
	
	/**
	 * Returns true if the given class is stored with a floating point representation,
	 * the only classes that are are float, double and their respective extended classes.<br>
	 * Note that for any non-primitive type this check will return false, this does not assure
	 * that the class is a primitive.
	 * 
	 * @param clazz the type to check
	 * @return true if clazz is either float, Float, double or Double.
	 */
	public static boolean isFloatingPoint(Class<?> clazz) {
		return  clazz == float.class || clazz == Float.class ||
				clazz == double.class || clazz == Double.class;
	}
	
	/**
	 * Returns true if the given class is one of {@link #EXTENDED_PRIMITIVE_TYPES}.
	 * 
	 * @param clazz the class to check
	 * @return true if the given class is one of {@link #EXTENDED_PRIMITIVE_TYPES}
	 */
	public static boolean isExtendedPrimitive(Class<?> clazz) {
		// the trivial loop can be easily unrolled so we don't iterate over the
		// extended primitive types array.
		return  clazz == Byte.class      ||
				clazz == Short.class     ||
				clazz == Integer.class   ||
				clazz == Long.class      ||
				clazz == Float.class     ||
				clazz == Double.class    ||
				clazz == Character.class ||
				clazz == Boolean.class;
	}
	
	/**
	 * Returns true if the given class is a true primitive class.<br>
	 * All invocations to this method behave exactly the same as <code>clazz.isPrimitive()</code>
	 * 
	 * @param clazz the class to check
	 * @return true if the class is a true primitive type
	 * @see #TRUE_PRIMITIVE_TYPES
	 */
	public static boolean isTruePrimitive(Class<?> clazz) {
		return clazz.isPrimitive();
	}
	
	/**
	 * Returns true if one of {@link #isTruePrimitive(Class)}, {@link #isExtendedPrimitive(Class)}
	 * returns true for this {@code Class} object.
	 * 
	 * @param clazz the class to check
	 * @return true if the class denotes a primitive or extended primitive type
	 * @see #PRIMITIVE_TYPES
	 */
	public static boolean isPrimitiveType(Class<?> clazz) {
		return isTruePrimitive(clazz) || isExtendedPrimitive(clazz);
	}
	
	/**
	 * Returns the <i>true</i> primitive type associated with the given extended type.<br>
	 * If the given type is already a primitive type it is returned as-is.
	 * 
	 * @param clazz the extended primitive type
	 * @return the {@link #TRUE_PRIMITIVE_TYPES true primitive type} associated with the given class
	 * @see #PRIMITIVE_TYPES
	 * @see #getExtendedPrimitiveType(Class)
	 */
	public static Class<?> getTruePrimitiveType(Class<?> clazz) {
		if(!isExtendedPrimitive(clazz))
			return clazz;
		for(int i = 0; i < EXTENDED_PRIMITIVE_TYPES.length; i++)
			if(EXTENDED_PRIMITIVE_TYPES[i] == clazz)
				return TRUE_PRIMITIVE_TYPES[i];
		throw new UnreachableException();
	}

	/**
	 * Returns the extended primitive type associated with the given <i>true</i> primitive type.<br>
	 * If the given class is not a true primitive type it is returned as-is.
	 * 
	 * @param clazz the true primitive type
	 * @return the {@link #EXTENDED_PRIMITIVE_TYPES extended primitive type} associated with the given class
	 * @see #PRIMITIVE_TYPES
	 * @see #getTruePrimitiveType(Class)
	 */
	public static Class<?> getExtendedPrimitiveType(Class<?> clazz) {
		if(!isTruePrimitive(clazz))
			return clazz;
		for(int i = 0; i < TRUE_PRIMITIVE_TYPES.length; i++)
			if(TRUE_PRIMITIVE_TYPES[i] == clazz)
				return EXTENDED_PRIMITIVE_TYPES[i];
		throw new UnreachableException();
	}
	
	/**
	 * Allows for conversions between extended primitive types.<br>
	 * <p>For example, all these statements will return true:
	 * <blockquote><pre>
	 * castToPrimitive(Integer.valueOf(4), float.class).equals(Float.valueOf(4.0f))
	 * castToPrimitive(Double.valueOf(4.5), Byte.class).equals(Byte.valueOf(4))
	 * <pre></blockquote>
	 * <p>{@code o} is first casted to {@link Long} or {@link Double} to avoid
	 * precision losses and then casted to the given type. This method will
	 * return an object (not a true primitive) which can be immediately to the
	 * corresponding true primitive type.
	 * <p>A precision loss may occur if the target primitive type is shorter than
	 * the type of {@code o} or if it is not floating point representation but
	 * the type of {@code o} is.
	 * 
	 * @param o the primitive value (of class one of {@link #PRIMITIVE_TYPES})
	 * @param primitiveType the true or extended primitive type
	 * @return o, casted to the specified <b>extended</b> primitive type without
	 * 			precision loss
	 * @see #PRIMITIVE_TYPES
	 */
	public static Object castToPrimitive(Object o, Class<?> primitiveType) throws IllegalArgumentException {
		Class<?> clazz = getTruePrimitiveType(primitiveType);
		if(clazz == null)
			throw new IllegalArgumentException("Class " + primitiveType + " is not a primitive type");
		if(clazz == boolean.class) {
			return (Boolean) o;
		} else if(isFloatingPoint(clazz)) {
			Double d = asDouble(o);
			if(clazz == double.class)
				return d.doubleValue();
			else if(clazz == float.class)
				return d.floatValue();
		} else {
			Long l = asLong(o);
			if(clazz == int.class)
				return l.intValue();
			else if(clazz == long.class)
				return l.longValue();
			else if(clazz == short.class)
				return l.shortValue();
			else if(clazz == byte.class)
				return l.byteValue();
			else if(clazz == char.class)
				return (char) l.byteValue();
		}
		throw new UnreachableException("Unexcpected type " + clazz);
	}
	
	/**
	 * Returns the long value of a primitive object.<br>
	 * If {@code o} is passed as a primitive, it will be returned as if 
	 * {@code (long) o} had been executed. If the class of {@code o} is
	 * one of {@link #EXTENDED_PRIMITIVE_TYPES} it is casted (with precision
	 * loss for floating point types) to a long.
	 * 
	 * @param o the primitive to cast to a long
	 * @return the long value of {@code o}
	 * @throws IllegalArgumentException if {@code o.getClass()} is not one
	 *         of {@link #PRIMITIVE_TYPES}
	 */
	public static long asLong(Object o) throws IllegalArgumentException {
		Class<?> clazz = o.getClass();
		if(clazz == Integer.class)
			return (int) o;
		else if(clazz == Long.class)
			return (long) o;
		else if(clazz == Float.class)
			return (long) (float) o;
		else if(clazz == Double.class)
			return (long) (double) o;
		else if(clazz == Short.class)
			return (short) o;
		else if(clazz == Byte.class)
			return (byte) o;
		else if(clazz == Character.class)
			return (char) o;
		else
			throw new IllegalArgumentException("Class " + clazz + " is not an extended primitive type");
	}
	
	/**
	 * Returns the double value of a primitive object.<br>
	 * If {@code o} is passed as a primitive, it will be returned as if 
	 * {@code (double) o} had been executed. If the class of {@code o} is
	 * one of {@link #EXTENDED_PRIMITIVE_TYPES} it is casted (possibly with
	 * precision loss) to a double.
	 * 
	 * @param o the primitive to cast to a double
	 * @return the double value of {@code o}
	 * @throws IllegalArgumentException if {@code o.getClass()} is not one
	 *         of {@link #PRIMITIVE_TYPES}
	 */
	public static double asDouble(Object o) throws IllegalArgumentException {
		Class<?> clazz = o.getClass();
		if(clazz == Integer.class)
			return (int) o;
		else if(clazz == Long.class)
			return (long) o;
		else if(clazz == Float.class)
			return (long) (float) o;
		else if(clazz == Double.class)
			return (long) (double) o;
		else if(clazz == Short.class)
			return (short) o;
		else if(clazz == Byte.class)
			return (byte) o;
		else if(clazz == Character.class)
			return (char) o;
		else
			throw new IllegalArgumentException("Class " + clazz + " is not an extended primitive type");
	}
	
	/**
	 * Creates a new {@code int} array and casts all objects of {@code array}
	 * to an int. This method may be used to convert any array with a component
	 * type in {@link #EXTENDED_PRIMITIVE_TYPES} (i.e: {@code Integer[]}) into
	 * an {@code int[]}.
	 * 
	 * @param array an array of primitive objects
	 * @return a new {@code int} array containing all values of {@code array}
	 * @throws ClassCastException if one of the values of {@code array}
	 *         cannot be casted to an int (note that {@code null} cannot be casted to
	 *         any primitive)
	 */
	public static int[] toIntArray(Object[] array) throws ClassCastException {
		int[] ints = new int[array.length];
		for(int i = 0; i < array.length; i++)
			ints[i] = (int) array[i];
		return ints;
	}
	
	/**
	 * Creates a new object array and fills it with the values of {@code array}.<br>
	 * This method may be used to convert a primitive array (i.e: {@code int[]} to
	 * the respective extended primitive object array (i.e: {@code Integer[]}.<br>
	 * This method works recursively so that {@code int[][]} will be casted to
	 * {@code Integer[][]}, the returned array can therefore safely be casted to
	 * a primitive array with the right number of dimensions.
	 * 
	 * @param array the primitive array
	 * @return an array of extended primitives
	 * @throws ClassCastException if one of the values of {@code array} cannot be
	 *         casted (note that {@code null} cannot be casted to any primitive)
	 * @see #EXTENDED_PRIMITIVE_TYPES
	 */
	public static Object[] toObjectArray(Object array) throws ClassCastException {
		Class<?> clazz = array.getClass().componentType();
		if(clazz == null)
			throw new IllegalArgumentException("Class " + clazz + " is not an array");
		if(clazz.isArray()) {
			Object[] x = new Object[Array.getLength(array)];
			for(int i = 0; i < x.length; i++)
				x[i] = toObjectArray(Array.get(array, i));
			return x;
		} else if(!isTruePrimitive(clazz)) {
			return (Object[]) array;
		} else if(clazz == int.class) {
			int[] x = (int[]) array;
			Object[] v = new Integer[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == float.class) {
			float[] x = (float[]) array;
			Object[] v = new Float[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == long.class) {
			long[] x = (long[]) array;
			Object[] v = new Long[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == short.class) {
			short[] x = (short[]) array;
			Object[] v = new Float[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == byte.class) {
			byte[] x = (byte[]) array;
			Object[] v = new Byte[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == double.class) {
			double[] x = (double[]) array;
			Object[] v = new Double[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == char.class) {
			char[] x = (char[]) array;
			Object[] v = new Character[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		} else if(clazz == boolean.class) {
			boolean[] x = (boolean[]) array;
			Object[] v = new Boolean[x.length];
			for(int i = 0; i < x.length; i++)
				v[i] = x[i];
			return v;
		}
		throw new UnreachableException("Unexpected type " + clazz);
	}
	
	public static void setPrimitive(Object o, Field f, Number n) throws IllegalAccessException {
		Class<?> type = f.getType();
		if(!type.isPrimitive())
			throw new IllegalArgumentException("Field " + f + " is not a primitive type");
		if(type == int.class)
			f.setInt(o, n.intValue());
		else if(type == float.class)
			f.setFloat(o, n.floatValue());
		else if(type == double.class)
			f.setDouble(o, n.doubleValue());
		else if(type == byte.class)
			f.setByte(o, n.byteValue());
		else if(type == long.class)
			f.setLong(o, n.longValue());
		else if(type == short.class)
			f.setShort(o, n.shortValue());
		else if(type == char.class)
			f.setChar(o, (char) n.byteValue());
		else
			throw new UnreachableException("Unexpected type " + type);
	}

	/**
	 * Return {@code true} if {@code parent} is assignable from {@code child}.
	 * It can be that <code>parent.isAssignableFrom(child)</code> or that both
	 * types represent the same primitive type (be they extended or true primitives)
	 * 
	 * @param parent the parent class
	 * @param child the child class
	 * @return {@code true} if {@code child} can be stored in a field of type {@code parent}
	 * @see #isSameExtendedType(Class, Class)
	 */
	public static boolean isAssignableFrom(Class<?> parent, Class<?> child) {
		if(!isPrimitiveType(parent))
			return parent.isAssignableFrom(child);
		return isSameExtendedType(parent, child);
	}
	
	/**
	 * Return {@code true} if {@code c1} and {@code c2} are the same class instance
	 * or are of the same primitive type (like Integer.TYPE (int.class) and
	 * Integer.class)
	 * 
	 * @param c1 first type to check
	 * @param c2 second type to check
	 * @return {@code true} if both type represent the same extended type
	 */
	public static boolean isSameExtendedType(Class<?> c1, Class<?> c2) {
		return getExtendedPrimitiveType(c1) == getExtendedPrimitiveType(c2);
	}
	
}
