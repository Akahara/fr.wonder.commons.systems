package fr.wonder.commons.systems.process.argparser;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.wonder.commons.systems.reflection.PrimitiveUtils;
import fr.wonder.commons.systems.reflection.ReflectUtils;
import fr.wonder.commons.utils.StringUtils;

public class ProcessArguments {
	
	private static final String HELP_OPTION = "--help";
	
	private final Branch tree = new Branch();
	private final Map<Class<?>, OptionsClass> optionClasses = new HashMap<>();
	
	private final String programName;
	
	public ProcessArguments(String programName, Class<?> entryPointClass) {
		this(programName);
		addEntryPoint(entryPointClass);
	}
	
	public ProcessArguments(String programName) {
		this.programName = programName;
	}
	
	public ProcessArguments addEntryPoint(Class<?> entryPointClass) {
		for(Method m : entryPointClass.getDeclaredMethods()) {
			EntryPoint annotation = m.getAnnotation(EntryPoint.class);
			if(annotation == null)
				continue;
			
			String path = annotation.path();
			
			try {
				Branch branch = getEntrylessBranch(path);
				validateEntryMethodParameters(m);
				OptionsClass opt = getOptionsClass(m);
				branch.entryPoint = EntryPointFunction.getEntryPointFunction(m, opt);
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
				throw new IllegalStateException("Cannot register branch '" + path + "' for method " + m.getName(), e);
			}
		}
		if(tree.subBranches.isEmpty() && tree.entryPoint == null)
			throw new IllegalStateException("Class " + entryPointClass + " contains no entry points");
		return this;
	}

	private Branch getEntrylessBranch(String path) throws IllegalArgumentException {
		String[] parts = path.split(" ");
		Branch current = tree;
		int pl = 0;
		
		// begin at 1, skip the program name
		for(int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if(!canBeBranchName(p))
				throw new IllegalArgumentException("Name " + p + " cannot be used as a branch path");
			
			if(current.entryPoint != null)
				throw new IllegalArgumentException("Branch '" + path.substring(0, pl) + "' has a declared entry point, it cannot have sub-paths");
			current = current.subBranches.computeIfAbsent(p, _p -> new Branch());
			pl += p.length()+1;
		}
		
		return current;
	}
	
	private void validateEntryMethodParameters(Method method) throws NoSuchMethodException, SecurityException {
		if(!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed statically");
		if(!method.canAccess(null))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed");
		Parameter[] params = method.getParameters();
		
		for(int i = 1; i < params.length; i++) {
			if(!canBeArgumentType(params[i].getType()))
				throw new IllegalArgumentException("Argument " + params[i].getName() +
						" has an invalid type " + params[i].getType().getName());
		}
	}
	
	private OptionsClass getOptionsClass(Method method) throws NoSuchMethodException, SecurityException {
		if(!doesMethodUseOptions(method))
			return null;
		Class<?> optionsType = method.getParameterTypes()[0];
		OptionsClass optionsClass = optionClasses.get(optionsType);
		if(optionsClass == null)
			optionsClass = OptionsClass.getOptions(optionsType);
		optionClasses.put(optionsType, optionsClass);
		return optionsClass;
	}
	
	public static boolean canBeBranchName(String text) {
		return text.matches("[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?");
	}
	
	public static boolean canBeOptionName(String text) {
		return text.matches("\\-\\-[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?") && !text.equals(HELP_OPTION);
	}
	
	public static boolean canBeOptionShortand(String text) {
		return text.matches("\\-[a-zA-Z]");
	}
	
	static boolean canBeArgumentType(Class<?> type) {
		return type == String.class || type == File.class || type.isEnum() || PrimitiveUtils.isPrimitiveType(type);
	}
	
	public static boolean doesMethodUseOptions(Method method) {
		Parameter[] params = method.getParameters();
		return params.length > 0 && !canBeArgumentType(params[0].getType());
	}
	
	public void eval(String[] args) throws IllegalArgumentException {
		if(args == null || args.length == 0)
			throw new IllegalArgumentException("Missing arguments");
		
		if(tree.subBranches.isEmpty())
			throw new IllegalStateException("No entry point registered");
		
		Map<String, String> options = new HashMap<>();
		List<String> argumentsStrings = new ArrayList<>();
		
		Branch branch = readArguments(args, options, argumentsStrings);
		
		if(args.length > 0 && args[0].equals(HELP_OPTION))
			printHelp(args, branch);
		else
			runCommand(branch.entryPoint, options, argumentsStrings);
	}
	
	public void eval(String args) throws IllegalArgumentException {
		eval(StringUtils.splitWithQuotes(args, " "));
	}
	
	public void run(String[] args) {
		try {
			eval(args);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public void run(String args) {
		run(StringUtils.splitWithQuotes(args, " "));
	}

	public static void runHere(String programName, String[] args) {
		new ProcessArguments(programName, ReflectUtils.getCallerClass()).run(args);
	}
	
	public static void runHere(String programName, String args) {
		new ProcessArguments(programName, ReflectUtils.getCallerClass()).run(args);
	}
	
	private void printHelp(String[] args, Branch branch) {
		if(branch.entryPoint != null) {
			System.out.println("Usage: " + getEntryUsage(branch.entryPoint));
			if(branch.entryPoint.options != null) {
				for(Field optField : new HashSet<>(branch.entryPoint.options.optionFields.values())) {
					ProcessOption opt = optField.getAnnotation(ProcessOption.class);
					System.out.print("  " + opt.name());
					if(!opt.shortand().isBlank())
						System.out.print(" (" + opt.shortand() + ")");
					if(optField.getType() != boolean.class)
						System.out.print(" <" + opt.valueName() + ">");
					if(!opt.desc().isEmpty())
						System.out.print("  - " + opt.desc());
					System.out.println();
				}
			}
		} else {
			System.out.println("Usage: " + String.join(" ", args) + " " 
					+ StringUtils.join("|", branch.subBranches.keySet()));
		}
	}
	
	private static void runCommand(EntryPointFunction entry, Map<String, String> options, List<String> argumentsStrings) {
		Object[] arguments = Arrays.copyOf(entry.defaultValues, entry.defaultValues.length);
		
		if(entry.options != null)
			arguments[0] = OptionsHelper.createOptionsInstance(options, entry.options);
		
		int entryOptOffset = entry.optionsOffset();
		for(int i = 0; i < argumentsStrings.size(); i++) {
			Parameter p = entry.method.getParameters()[i+entryOptOffset];
			arguments[i+entryOptOffset] = OptionsHelper.parseOptionValue(
					argumentsStrings.get(i),
					p.getType(),
					entry.argumentsAnnotations[i].name());
		}
		
		try {
			entry.method.invoke(null, arguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to invoke method " + entry.method.getName(), e);
		}
	}
	
	private Branch readArguments(String[] args, Map<String, String> outOptions, List<String> outArguments) {
		Branch currentBranch = tree;
		
		int begin = 0;
		if(args.length > 0 && (args[0].equals("?") || args[0].equals(HELP_OPTION))) {
			args[0] = HELP_OPTION;
			begin++; // skip --help
		}
		
		for(int i = begin; i < args.length; i++) {
			String arg = args[i];
			
			if(arg.isBlank())
				throw new IllegalArgumentException("Unexpected blank space");
			
			if(arg.startsWith("-")) {
				if(i == args.length-1) {
					outOptions.put(arg, null);
					break;
				}
				String nextArg = args[++i];
				if(currentBranch.subBranches.containsKey(nextArg)) {
					currentBranch = currentBranch.subBranches.get(nextArg);
					outOptions.put(arg, null);
				} else {
					outOptions.put(arg, nextArg);
				}
			} else if(currentBranch.entryPoint == null) {
				if(currentBranch.subBranches.containsKey(arg)) {
					currentBranch = currentBranch.subBranches.get(arg);
				} else {
					String current = i == 0 ? programName : programName + " " + String.join(" ", Arrays.copyOfRange(args, 0, i));
					throw new IllegalArgumentException("Unknown option " + arg + ".\nUsage: " 
							+ current + " " + StringUtils.join("|", currentBranch.subBranches.keySet()));
				}
			} else {
				outArguments.add(arg);
			}
		}
		
		if(args.length > 1 && args[1].equals(HELP_OPTION))
			return currentBranch;
		
		EntryPointFunction entry = currentBranch.entryPoint;
		
		if(entry == null) {
			throw new IllegalArgumentException("Usage: " + programName + " " + String.join(" ", args)
					+ " " + StringUtils.join("|", currentBranch.subBranches.keySet()));
		}

		if(outArguments.size() + entry.optionalArgsCount < entry.paramCount() || outArguments.size() > entry.paramCount())
			throw new IllegalArgumentException("Usage: " + getEntryUsage(entry));
		
		if(entry.options == null && !outOptions.isEmpty())
			throw new IllegalArgumentException("Unexpected option: " + outOptions.keySet().toArray()[0]);
		
		return currentBranch;
	}
	
	private String getEntryUsage(EntryPointFunction entry) {
		Method entryMethod = entry.method;
		String usage = programName + " " + entryMethod.getAnnotation(EntryPoint.class).path();
		for(int i = 0; i < entry.paramCount(); i++) {
			if(!entry.argumentsAnnotations[i].defaultValue().isEmpty())
				usage += " [" + entry.argumentsAnnotations[i].name() + "]";
			else
				usage += " <" + entry.argumentsAnnotations[i].name() + ">";
		}
		return usage;
	}
	
}
