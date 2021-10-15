package fr.wonder.commons.systems.process.argparser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.systems.reflection.ReflectUtils;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.StringUtils;

public class ArgParser {

	private final Branch treeRoot = new Branch();
	private final Map<Class<?>, OptionsClass> optionClasses = new HashMap<>();
	private final Map<String, Boolean> optionsTakingArguments = new HashMap<>();
	
	private final String progName;
	
	public ArgParser(String progName) {
		this.progName = progName;
	}
	
	public void run(String args) {
		run(StringUtils.splitWithQuotes(args, " "));
	}

	public static void runHere(String programName, String[] args) {
		new ArgParser(programName).addEntryPoints(ReflectUtils.getCallerClass()).run(args);
	}
	
	public static void runHere(String programName, String args) {
		new ArgParser(programName).addEntryPoints(ReflectUtils.getCallerClass()).run(args);
	}
	
	
	
	public ArgParser addEntryPoints(Class<?> entryPointClass) throws InvalidDeclarationError {
		for(Method m : entryPointClass.getDeclaredMethods()) {
			EntryPoint annotation = m.getAnnotation(EntryPoint.class);
			if(annotation == null)
				continue;
			
			String path = annotation.path();
			
			try {
				Branch branch = getEntrylessBranch(path);
				ArgParserHelper.validateEntryMethodParameters(m);
				OptionsClass opt = getOrCreateOptionClass(m);
				branch.entryPoint = EntryPointFunction.createEntryPointFunction(m, opt);
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
				throw new InvalidDeclarationError("Cannot register branch '" + path + "' for method " + m.getName(), e);
			}
		}
		if(treeRoot.subBranches.isEmpty() && treeRoot.entryPoint == null)
			throw new InvalidDeclarationError("Class " + entryPointClass + " contains no entry points");
		return this;
	}

	private Branch getEntrylessBranch(String path) throws InvalidDeclarationError {
		String[] parts = path.split(" ");
		Branch current = treeRoot;
		int pl = 0;
		
		for(String p : parts) {
			if(!ArgParserHelper.canBeBranchName(p))
				throw new InvalidDeclarationError("Name " + p + " cannot be used as a branch path");
			
			if(current.entryPoint != null)
				throw new InvalidDeclarationError("Branch '" + path.substring(0, pl) + "' has a declared entry point, it cannot have sub-paths");
			current = current.subBranches.computeIfAbsent(p, _p -> new Branch());
			pl += p.length()+1;
		}
		
		return current;
	}
	
	private OptionsClass getOrCreateOptionClass(Method method) throws InvalidDeclarationError {
		if(!ArgParserHelper.doesMethodUseOptions(method))
			return null;
		Class<?> optionsType = method.getParameterTypes()[0];
		OptionsClass optionsClass = optionClasses.get(optionsType);
		if(optionsClass == null)
			optionsClass = OptionsClass.createOptionsClass(optionsType);
		for(Entry<String, Field> option : optionsClass.optionFields.entrySet()) {
			String optName = option.getKey();
			Boolean alreadyDefinedTakesArg = optionsTakingArguments.get(optName);
			boolean takesArg = OptionsHelper.doesOptionTakeArgument(option.getValue().getType());
			if(alreadyDefinedTakesArg != null && takesArg != alreadyDefinedTakesArg)
				throw new InvalidDeclarationError("Option '" + optName + "' was defined in two option classes,"
						+ " only one taking an argument: second occurence" + option.getValue());
			optionsTakingArguments.put(optName, takesArg);
		}
		optionClasses.put(optionsType, optionsClass);
		return optionsClass;
	}
	
	
	
	public void run(String[] args) {
		if(treeRoot.subBranches.isEmpty())
			throw new IllegalStateException("No entry point registered");
		
		Map<String, String> options = new HashMap<>();
		List<String> entryArguments = new ArrayList<>();
		Branch entryPointBranch;
		
		List<String> arguments = args == null ? Collections.emptyList() : new ArrayList<>(Arrays.asList(args));
		boolean isHelpPrint = !arguments.isEmpty() && ArgParserHelper.isHelpPrint(arguments.get(0));
		if(isHelpPrint) arguments.remove(0);
		
		try {
			ErrorWrapper errors = new ErrorWrapper("Invalid arguments", false);
			
			entryPointBranch = readArguments(errors, arguments, options, entryArguments);
			
			errors.assertNoErrors();
			
			EntryPointFunction entry = entryPointBranch.entryPoint;
			
			if(isHelpPrint) {
				if(entry == null) {
					System.out.println(getUnfinishedPathUsage(arguments, arguments.size(), entryPointBranch));
				} else {
					printEntryPointHelp(entry);
				}
			} else {
				if(entry == null) {
					errors.addAndThrow(getUnfinishedPathUsage(arguments, arguments.size(), entryPointBranch));
				} else if(entryArguments.size() + entry.optionalParamCount() < entry.paramCount()) {
					for(int i = entryArguments.size(); i < entry.paramCount() - entry.optionalParamCount(); i++)
						errors.add("Missing argument for <" + entry.getParamName(i) + ">");
					errors.addAndThrow(getEntryUsage(entry));
				} else if(entryArguments.size() > entry.paramCount()) {
					errors.addAndThrow("Too many arguments given\n" + getEntryUsage(entry));
				} else {
					runCommand(errors, entry, options, entryArguments);
				}
			}
		} catch (WrappedException e) {
			e.errors.dump();
		}
	}
	
	private void printEntryPointHelp(EntryPointFunction entryPoint) {
		System.out.println(getEntryUsage(entryPoint));
		for(int i = 0; i < entryPoint.paramCount(); i++) {
			Class<?> argConcreteType = entryPoint.getParamType(i);
			String argName = entryPoint.getParamName(i);
			String argType = argConcreteType.isEnum() ?
					StringUtils.join("|", argConcreteType.getEnumConstants()) :
					argConcreteType.getSimpleName();
			String argDesc = entryPoint.getParamDesc(i);
			if(!argDesc.isBlank())
				argDesc = "  - " + argDesc.replaceAll("\n", "    ");
			System.out.println("  " + argName + " (" + argType + ")" + argDesc);
		}
		if(entryPoint.options != null) {
			for(Field optField : new HashSet<>(entryPoint.options.optionFields.values())) {
				Option opt = optField.getAnnotation(Option.class);
				System.out.print("  " + opt.name());
				if(!opt.shortand().isBlank())
					System.out.print(" (" + opt.shortand() + ")");
				if(OptionsHelper.doesOptionTakeArgument(optField.getType()))
					System.out.print(" <" + opt.valueName() + ">");
				if(!opt.desc().isEmpty())
					System.out.print("  - " + opt.desc());
				System.out.println();
			}
		}
	}
	
	private static void runCommand(ErrorWrapper errors, EntryPointFunction entry,
			Map<String, String> options, List<String> argumentsStrings) throws WrappedException {
		
		Object[] arguments = new Object[entry.paramCount()];
		
		for(int i = 0; i < argumentsStrings.size(); i++) {
			try {
				arguments[i] = OptionsHelper.parseOptionValue(
						argumentsStrings.get(i),
						entry.getParamType(i),
						entry.getParamName(i));
			} catch (ArgumentError e) {
				errors.add(e.getMessage());
			}
		}
		
		arguments = entry.finishArgsArray(arguments, options, errors);
		
		errors.assertNoErrors();
		
		try {
			entry.method.invoke(null, arguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if(e.getCause() instanceof Error) {
				cleanStackTrace(e.getCause());
				throw (Error) e.getCause();
			}
			if(e.getCause() instanceof RuntimeException) {
				cleanStackTrace(e.getCause());
				throw (RuntimeException) e.getCause();
			}
			throw new IllegalStateException("Unable to invoke method " + entry.method, e);
		}
	}
	
	/**
	 * Clean stack trace of uncaught exceptions thrown by the entry point method.
	 * 
	 * <p>
	 * This method removes stack trace elements that come from this class, the
	 * Method class and some jdk internal classes responsible for method invocations
	 * through the reflect api.
	 * 
	 * <p>
	 * It is used by the {@code run} functions of this class to make the trace
	 * clearer, the trace won't contain references to this class but the first call
	 * to a {@code run} function will still appear.
	 * 
	 * <p>
	 * This method may have undesired results if the entry point method uses the
	 * reflect api and does not catch invocation errors, as this method will also
	 * remove these calls from the final stack trace.
	 */
	private static void cleanStackTrace(Throwable t) {
		StackTraceElement[] trace = t.getStackTrace();
		Set<String> classNames = Set.of(
				ArgParser.class.getName(),
				Method.class.getName(),
				"jdk.internal.reflect.NativeMethodAccessorImpl",      // filter out inaccessible classes, may not have
				"jdk.internal.reflect.DelegatingMethodAccessorImpl"); // effect depending on JDK implementation maybe ?
		t.setStackTrace(ArrayOperator.filter(trace, el -> !classNames.contains(el.getClassName())));
	}
	
	
	
	private Branch readArguments(ErrorWrapper errors, List<String> args, Map<String, String> outOptions,
			List<String> outArguments) {
		
		Branch currentBranch = treeRoot;
		
		boolean loggedPathError = false;
		
		for(int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			
			if(arg.startsWith("-")) {
				readOptionArg(i--, args, outOptions, errors);
				
			} else if(currentBranch.entryPoint != null) {
				outArguments.add(arg);
				args.remove(i--);
				
			} else {
				if(currentBranch.subBranches.containsKey(arg)) {
					currentBranch = currentBranch.subBranches.get(arg);
				} else if(!loggedPathError) {
					errors.add("Unknown usage - " + arg + "\n" 
							+ getUnfinishedPathUsage(args, i, currentBranch));
					loggedPathError = true;
				}
			}
		}
		
		return currentBranch;
	}
	
	private void readOptionArg(int position, List<String> args, Map<String, String> outOptions, ErrorWrapper errors) {
		String option = args.remove(position);
		
		// read combined notation -abc
		if(!option.startsWith("--")) {
			String[] chars = option.split("");
			for(int i = 1; i < chars.length-1; i++) {
				String copt = "-" + chars[i];
				Boolean takesArgument = optionsTakingArguments.get(option);
				if(takesArgument != null && takesArgument) {
					errors.add("Option " + copt + " requires a value");
				} else {
					outOptions.put(copt, null);
				}
			}
			option = "-" + chars[chars.length-1];
		}
		
		Boolean takesArgument = optionsTakingArguments.get(option);
		if(takesArgument != null && takesArgument) {
			if(position == args.size()) {
				errors.add("Option " + option + " requires a value");
			} else {
				String nextArg = args.remove(position);
				outOptions.put(option, nextArg);
			}
		} else {
			outOptions.put(option, null);
		}
	}
	
	private String getUnfinishedPathUsage(List<String> args, int readCount, Branch currentBranch) {
		return "Usage: " + getCurrentPathString(args, readCount) + " " 
				+ StringUtils.join("|", currentBranch.subBranches.keySet()) + " ...\nUse '" + progName + " --help <cmd>' for help";
	}
	
	private String getCurrentPathString(List<String> args, int readCount) {
		String s = progName;
		for(int i = 0; i < readCount; i++)
			s += " " + args.get(i);
		return s;
	}
	
	private String getEntryUsage(EntryPointFunction entry) {
		Method entryMethod = entry.method;
		String usage = "Usage: " + progName;
		if(entry.options != null) {
			Collection<String> availableOptions = entry.options.getAvailableOptionNames();
			if(availableOptions.size() > 3) {
				usage += " (...options)";
			} else {
				for(String opt : availableOptions)
					usage += " (" + opt + ")";
			}
		}
		usage += " " + entryMethod.getAnnotation(EntryPoint.class).path();
		int i = 0;
		for( ; i < entry.optionalParamCount(); i++)
			usage += " <" + entry.getParamName(i) + ">";
		for( ; i < entry.paramCount(); i++)
			usage += " [" + entry.getParamName(i) + "]";
		return usage;
	}
	
}
