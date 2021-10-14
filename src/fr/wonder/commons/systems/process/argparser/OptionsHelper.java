package fr.wonder.commons.systems.process.argparser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;

import fr.wonder.commons.exceptions.ErrorWrapper;
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
				} catch (IllegalArgumentException e) {
					throw new ArgumentError("Expected double value for <" + argName + ">, got '" + arg + "'");
				}
			} else {
				try {
					return PrimitiveUtils.castToPrimitive(Long.parseLong(arg), argType);
				} catch (IllegalArgumentException e) {
					throw new ArgumentError("Expected integer value for <" + argName + ">, got '" + arg + "'");
				}
			}
		} else if(argType == String.class) {
			return arg;
		} else if(argType == File.class) {
			try {
				return new File(arg).getCanonicalFile();
			} catch (IOException e) {
				throw new ArgumentError("Cannot resolve path " + arg + ": " + e.getMessage());
			}
		} else if(argType.isEnum()) {
			try {
				return ReflectUtils.getEnumConstant(argType, arg.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ArgumentError("Expected one of " + StringUtils.join("|", argType.getEnumConstants()) +
						" for <" + argName + ">, got '" + arg + "'");
			}
		} else {
			throw new UnreachableException("Invalid option type " + argType);
		}
	}

	static Object createOptionsInstance(Map<String, String> rawOptions, OptionsClass options, ErrorWrapper errors) {
		Object instance = options.newInstance();
		
		for(Entry<String, String> optPair : rawOptions.entrySet()) {
			Field optField = options.optionFields.get(optPair.getKey());
			if(optField == null)
				errors.add("Unknown option: " + optPair.getKey());
			else
				setOption(instance, optField, optPair.getKey(), optPair.getValue(), errors);
		}
		
		return instance;
	}

	private static void setOption(Object optionObj, Field optionField, String opt, String value, ErrorWrapper errors) {
		try {
			Class<?> optionType = optionField.getType();
			
			if(optionType == boolean.class) {
				optionField.setBoolean(optionObj, !optionField.getBoolean(optionObj));
				return;
			}
			
			Object argVal;
			try {
				argVal = parseOptionValue(value, optionType, opt);
			} catch (ArgumentError e) {
				errors.add(e.getMessage());
				return;
			}
			
			if(PrimitiveUtils.isTruePrimitive(optionType)) {
				PrimitiveUtils.setPrimitive(optionObj, optionField, (Number) argVal);
			} else {
				optionField.set(optionObj, argVal);
			}
		} catch (IllegalAccessException e) {
			errors.add("Cannot Cannot set an option field value: " + e.getMessage());
		}
	}

	public static boolean doesOptionTakeArgument(Class<?> type) {
		return type != boolean.class;
	}

}
