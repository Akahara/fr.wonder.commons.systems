package fr.wonder.commons.systems.debug;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DebugValues {
	
	public static int[] firstNIntegers(int n) {
		return IntStream.range(0, n).toArray();
	}
	
	public static int[] randomIntArray(int n, int min, int max) {
		return Stream.generate(Math::random).limit(n).mapToInt(i -> (int) (i*(max-min+1)+min)).toArray();
	}
	
	public static double[] randomDoubleArray(int length) {
		return Stream.generate(Math::random).limit(length).mapToDouble(d->d).toArray();
	}
	
	public static float[] randomFloatArray(int length) {
		float[] array = new float[length];
		for(int i = 0; i < length; i++)
			array[i] = (float) Math.random();
		return array;
	}
	
	public static Object randomMatrix(int... dimensions) {
		int size = dimensions[0];
		if(dimensions.length == 1)
			return randomDoubleArray(size);
		int[] subdimensions = Arrays.copyOfRange(dimensions, 1, dimensions.length);
		Object array = Array.newInstance(Double.TYPE, dimensions);
		for(int i = 0; i < size; i++)
			Array.set(array, i, randomMatrix(subdimensions)); 
		return array;
	}
	
	public static int[] generateIntArray(IntUnaryOperator generator, int length) {
		return IntStream.range(0, length).map(generator).toArray();
	}
	
}
