package fr.wonder.commons.systems.process;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.systems.reflection.PrimitiveUtils;
import fr.wonder.commons.systems.reflection.ReflectUtils;
import fr.wonder.commons.utils.Assertions;
import fr.wonder.commons.utils.StringUtils;

public class ProcessArguments {

	public final Branch mainBranch = new Branch(null);
	private Class<?> optionsClass;
	private final Map<String, Field> optionFields = new HashMap<>();
	
	public ProcessArguments() {
		
	}
	
	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
		ProcessArguments pargs = new ProcessArguments();
		pargs.setOptions(Options.class)
			.begin()
			.branch("branch0")
				.bind(ProcessArguments.class.getMethod("a", Options.class, int.class))
			.branch("branch1")
				.bind((opt) -> System.out.println(">"));
		pargs.parse(new String[] {"process", "branch0", "5"});
	}
	
	public static class Options {
		
		@ProcessOption(name = "--abc")
		public int val;
		
	}
	
	public static void a(Options opt, int i) {
		System.out.println("called a with " + i);
	}
	
	public ProcessArguments setOptions(Class<?> options) {
		this.optionsClass = options;
		if(options == null)
			return this;
		try {
			if(!options.getDeclaredConstructor().canAccess(null))
				throw new SecurityException();
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Class " + options + " does not declare an empty constructor");
		}
		for(Field f : options.getDeclaredFields()) {
			ProcessOption opt = f.getAnnotation(ProcessOption.class);
			if(opt != null) {
				String name = opt.name();
				if(!canBeOptionName(name))
					throw new IllegalArgumentException("Name " + name + " cannot be an option on field " + f);
				if(optionFields.put(name, f) != null)
					throw new IllegalArgumentException("Name " + name + " specified twice on field " + f);
				String shortand = opt.shortand();
				if(!shortand.equals("null")) {
					if(!canBeOptionShortand(shortand))
						throw new IllegalArgumentException("Name " + shortand + " cannot be a shortand on field " + f);
					if(optionFields.put(shortand, f) != null)
						throw new IllegalArgumentException("Name " + shortand + " specified twice on field " + f);
				}
				Class<?> type = f.getType();
				if(type != String.class && type != File.class && !PrimitiveUtils.isTruePrimitive(type))
					throw new IllegalArgumentException("Option of field + " + f + " cannot be of type " + type);
			}
		}
		return this;
	}
	
	public Branch begin() {
		return mainBranch;
	}
	
	public static boolean canBeBranchName(String text) {
		return text.matches("[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?");
	}
	
	public static boolean canBeOptionName(String text) {
		return text.matches("\\-\\-[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?");
	}
	
	public static boolean canBeOptionShortand(String text) {
		return text.matches("\\-[a-zA-Z]");
	}
	
	public void parse(String[] args) {
		Branch currentBranch = mainBranch;
		Object optionObj = null;
		List<Object> arguments = new ArrayList<>();
		Method function = null;
		
		String currentPath = "";
		
		if(args == null || args.length == 0)
			throw new IllegalArgumentException("No arguments given");
		if(optionsClass != null) {
			try {
				optionObj = optionsClass.getConstructor().newInstance();
				arguments.add(optionObj);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException("Cannot instanciate the option class");
			}
		}
		
		Iterator<String> argsIterator = Arrays.asList(args).iterator();
		String arg0 = argsIterator.next();
		currentPath += arg0;
		
		while(argsIterator.hasNext()) {
			String arg = argsIterator.next();
			if(arg.isBlank())
				continue;
			if(arg.startsWith("-")) { // read option
				try {
					setOption(optionObj, arg, argsIterator);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException("Option class " + optionsClass + " is not operable", e);
				}
			} else if(currentBranch.function == null) { // read branch
				Branch nextBranch = currentBranch.subBranches.get(arg);
				if(nextBranch == null) {
					throw new IllegalArgumentException(
							"Expected one of " + StringUtils.join("|", currentBranch.subBranches.keySet()) + " got " + arg);
				}
				currentBranch = nextBranch;
				currentPath += " " + arg;
			} else { // read argument
				if(function == null) {
					function = currentBranch.function.getClass().getDeclaredMethods()[0];
				}
				System.out.println(Arrays.toString(arguments.toArray()));
				if(arguments.size() == function.getParameterCount())
					throw new IllegalArgumentException("Too many arguments, usage: " + getCommandUsage(currentPath, currentBranch));

				Parameter param = function.getParameters()[arguments.size()];
				arguments.add(parseValue(arg, param.getClass(), param.getName()));
			}
		}
		
		if(function == null)
			throw new IllegalArgumentException("Usage: " + getCommandUsage(currentPath, currentBranch));
		
		if(arguments.size() != function.getParameterCount())
			throw new IllegalArgumentException("Missing arguments, usage: " + getCommandUsage(currentPath, currentBranch));
		
		try {
			function.invoke(currentBranch.function, arguments.toArray());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException("Cannot invoke bound method of branch " + currentPath, e);
		}
	}
	
	private static String getCommandUsage(String path, Branch branch) {
		if(branch.function == null)
			return path + " " + StringUtils.join("|", branch.subBranches.keySet()) + " ...";
		Method function = branch.function.getClass().getDeclaredMethods()[0];
		return path + " " + StringUtils.join(" ", function.getParameters(), p -> p.getName());
	}
	
	private void setOption(Object optionObj, String arg, Iterator<String> args) throws IllegalAccessException {
		Field optionField = optionFields.get(arg);
		if(optionField == null)
			throw new IllegalArgumentException("Unknown option: " + arg);
		
		Class<?> optionType = optionField.getType();
		
		if(optionType.equals(boolean.class)) {
			optionField.setBoolean(optionObj, true);
			return;
		}
		
		if(!args.hasNext())
			throw new IllegalArgumentException("Missing value: " + arg);
		
		Object argVal = parseValue(args.next(), optionType, arg);
		
		if(PrimitiveUtils.isTruePrimitive(optionType)) {
			PrimitiveUtils.setPrimitive(optionObj, optionField, (Number) argVal);
		} else {
			optionField.set(optionObj, argVal);
		}
	}
	
	private Object parseValue(String arg, Class<?> argType, String argName) {
		if(PrimitiveUtils.isPrimitiveType(argType)) {
			if(PrimitiveUtils.isFloatingPoint(argType)) {
				try {
					return Double.parseDouble(arg);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Expected double value for " + argName + ", got " + arg);
				}
			} else {
				try {
					return Long.parseLong(arg);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Expected integer value for " + argName + ", got " + arg);
				}
			}
		} else if(argType == String.class) {
			return arg;
		} else if(argType == File.class) {
			return new File(arg);
		} else if(!argType.isEnum()) {
			Object enumConstant = ReflectUtils.getEnumConstant(argType, arg);
			if(enumConstant == null) {
				throw new IllegalArgumentException("Expected one of " + StringUtils.join("|", argType.getEnumConstants()) +
						" for " + argName + ", got " + arg);
			}
			return enumConstant;
		} else {
			throw new UnreachableException("Invalid option type " + argType);
		}
	}
	
	private static boolean canBeArgumentType(Class<?> type) {
		return type == String.class || type == File.class || type.isEnum() || PrimitiveUtils.isPrimitiveType(type);
	}
	
	public class Branch {
		
		private final Branch parent;
		private final Map<String, Branch> subBranches = new HashMap<>();
		private Object function;
		private String description;
		
		public Branch(Branch parent) {
			this.parent = parent;
		}
		
		public Branch branch(String text) {
			Assertions.assertTrue(canBeBranchName(text), text + " cannot be a branch name");
			Assertions.assertFalse(subBranches.containsKey(text), "Branch " + text + " is already set");
			Assertions.assertNull(function, "This branch already has arguments");
			Branch subBranch = new Branch(this);
			subBranches.put(text, subBranch);
			return subBranch;
		}
		
		private Branch bindFunction(Object function) {
			Assertions.assertNull(this.function, "This branch already has arguments");
			Assertions.assertEmpty(subBranches, "This branch already has sub-branches");
			Method[] methods = function.getClass().getDeclaredMethods();
			if(methods.length != 1 || !methods[0].canAccess(function))
				throw new IllegalArgumentException("Object " + function + " cannot be used as"
						+ " a binding as it does not declare a single accessible function");
			Method m = methods[0];
			Parameter[] params = m.getParameters();
			int beginIndex = optionsClass == null ? 0 : 1;
			System.out.println(m);
			if(params.length < beginIndex)
				throw new IllegalArgumentException("Function has no arguments but uses options");
			for(int i = beginIndex; i < params.length; i++) {
				if(!canBeArgumentType(params[i].getType()))
					throw new IllegalArgumentException("Argument " + params[i].getName() +
							" has an invalid type " + params[i].getType());
			}
			this.function = function;
			return end();
		}
		
		public Branch bind(Method method) { return bindFunction(method); }
		public Branch bind(Consumer<?> function) { return bindFunction(function); }
		public Branch bind(BiConsumer<?, ?> function) { return bindFunction(function); }
		public Branch bind(Callable3<?, ?, ?> function) { return bindFunction(function); }
		public Branch bind(Callable4<?, ?, ?, ?> function) { return bindFunction(function); }
		public Branch bind(Callable5<?, ?, ?, ?, ?> function) { return bindFunction(function); }
		public Branch bind(Callable6<?, ?, ?, ?, ?, ?> function) { return bindFunction(function); }

		public Branch description(String text) {
			this.description = text;
			return this;
		}
		
		public Branch end() {
			return parent;
		}
		
		public ProcessArguments finish() {
			return ProcessArguments.this;
		}
		
	}

	/** Can be used to cast static methods to objects */
	public static interface Callable0 extends Runnable {}
	/** Can be used to cast static methods to objects */
	public static interface Callable1<T1> extends Consumer<T1> {}
	/** Can be used to cast static methods to objects */
	public static interface Callable2<T1, T2> extends BiConsumer<T1, T2> {}
	/** Can be used to cast static methods to objects */
	public static interface Callable3<T1, T2, T3> { public void call(T1 t1, T2 t2, T3 t3); }
	/** Can be used to cast static methods to objects */
	public static interface Callable4<T1, T2, T3, T4> { public void call(T1 t1, T2 t2, T3 t3, T4 t4); }
	/** Can be used to cast static methods to objects */
	public static interface Callable5<T1, T2, T3, T4, T5> { public void call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5); }
	/** Can be used to cast static methods to objects */
	public static interface Callable6<T1, T2, T3, T4, T5, T6> { public void call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6); }
}
