package fr.wonder.commons.systems.process.argparser;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import fr.wonder.commons.systems.reflection.PrimitiveUtils;

public class ProcessArgumentsHelper {

	public static void validateEntryMethodParameters(Method method) throws NoSuchMethodException, SecurityException {
		if(!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed statically");
		if(!method.canAccess(null))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed");
		Parameter[] params = method.getParameters();
		
		for(int i = 1; i < params.length; i++) {
			if(!ProcessArgumentsHelper.canBeArgumentType(params[i].getType()))
				throw new IllegalArgumentException("Argument " + params[i].getName() +
						" has an invalid type " + params[i].getType().getName());
		}
	}

	public static boolean canBeBranchName(String text) {
		return text.matches("[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?") &&
				!isHelpPrint(text);
	}

	public static boolean canBeOptionName(String text) {
		return text.matches("\\-\\-[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?") &&
				!isHelpPrint(text);
	}

	public static boolean canBeOptionShortand(String text) {
		return text.matches("\\-[a-zA-Z]") &&
				!isHelpPrint(text);
	}

	public static boolean canBeArgumentType(Class<?> type) {
		return type == String.class || type == File.class || type.isEnum() || PrimitiveUtils.isPrimitiveType(type);
	}

	public static boolean doesMethodUseOptions(Method method) {
		Parameter[] params = method.getParameters();
		return params.length > 0 && !canBeArgumentType(params[0].getType());
	}

	public static boolean isHelpPrint(String arg) {
		return arg.equals("help") ||
				arg.equals("--help") ||
				arg.equals("?");
	}

}
