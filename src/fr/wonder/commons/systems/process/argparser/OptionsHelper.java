package fr.wonder.commons.systems.process.argparser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;

import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.systems.reflection.PrimitiveUtils;
import fr.wonder.commons.systems.reflection.ReflectUtils;
import fr.wonder.commons.utils.StringUtils;

class OptionsHelper {

	static Object parseOptionValue(String arg, Class<?> argType, String argName) throws ArgumentError {
		if(PrimitiveUtils.isPrimitiveType(argType)) {
			if(PrimitiveUtils.isFloatingPoint(argType)) {
				try {
					return PrimitiveUtils.castToPrimitive(Double.parseDouble(arg), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected double value for <" + argName + ">, got '" + arg + "'");
				}
			} else {
				try {
					return PrimitiveUtils.castToPrimitive(Long.parseLong(arg), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected integer value for <" + argName + ">, got '" + arg + "'");
				}
			}
		} else if(argType == String.class) {
			return arg;
		} else if(argType == File.class) {
			try {
				return new File(arg).getCanonicalFile();
			} catch (IOException | NullPointerException e) {
				throw new ArgumentError("Cannot resolve path " + arg + ": " + e.getMessage());
			}
		} else if(argType.isEnum()) {
			try {
				return ReflectUtils.getEnumConstant(argType, arg.toUpperCase());
			} catch (IllegalArgumentException | NullPointerException e) {
				throw new ArgumentError("Expected one of " + StringUtils.join("|", argType.getEnumConstants()) +
						" for <" + argName + ">, got '" + arg + "'");
			}
		} else {
			throw new UnreachableException("Invalid option type " + argType);
		}
	}

	static Object createOptionsInstance(Map<String, String> rawOptions, OptionsClass options) throws ArgumentError {
		Object instance = options.newInstance();
		
		for(Entry<String, String> optPair : rawOptions.entrySet()) {
			Field optField = options.optionFields.get(optPair.getKey());
			if(optField == null)
				throw new IllegalArgumentException("Unknown option: " + optPair.getKey());
			setOption(instance, optPair.getKey(), optPair.getValue(), optField);
		}
		
		return instance;
	}

	private static void setOption(Object optionObj, String opt, String value, Field optionField) throws ArgumentError {
		try {
			Class<?> optionType = optionField.getType();
			
			if(optionType.equals(boolean.class)) {
				if(value != null)
					throw new IllegalArgumentException("Superfluous option value: " + value + " for option " + opt);
				optionField.setBoolean(optionObj, true);
				return;
			}
			
			Object argVal;
				argVal = parseOptionValue(value, optionType, opt);
			
			if(PrimitiveUtils.isTruePrimitive(optionType)) {
				PrimitiveUtils.setPrimitive(optionObj, optionField, (Number) argVal);
			} else {
				optionField.set(optionObj, argVal);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot set an option field value", e);
		}
	}

}
