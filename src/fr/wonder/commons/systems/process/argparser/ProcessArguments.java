package fr.wonder.commons.systems.process.argparser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.wonder.commons.systems.reflection.ReflectUtils;
import fr.wonder.commons.utils.StringUtils;

public class ProcessArguments {
	
	private final Branch treeRoot = new Branch();
	private final Map<Class<?>, OptionsClass> optionClasses = new HashMap<>();
	
	private final String programName;
	
	public ProcessArguments(String programName) {
		this.programName = programName;
	}
	
	public ProcessArguments addEntryPoints(Class<?> entryPointClass)throws IllegalArgumentException {
		for(Method m : entryPointClass.getDeclaredMethods()) {
			EntryPoint annotation = m.getAnnotation(EntryPoint.class);
			if(annotation == null)
				continue;
			
			String path = annotation.path();
			
			try {
				Branch branch = getEntrylessBranch(path);
				ProcessArgumentsHelper.validateEntryMethodParameters(m);
				OptionsClass opt = getOrCreateOptionClass(m);
				branch.entryPoint = EntryPointFunction.getEntryPointFunction(m, opt);
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
				throw new IllegalArgumentException("Cannot register branch '" + path + "' for method " + m.getName(), e);
			}
		}
		if(treeRoot.subBranches.isEmpty() && treeRoot.entryPoint == null)
			throw new IllegalArgumentException("Class " + entryPointClass + " contains no entry points");
		return this;
	}
	
	public void run(String[] args) {
		if(treeRoot.subBranches.isEmpty())
			throw new IllegalStateException("No entry point registered");
		
		Map<String, String> options = new HashMap<>();
		List<String> entryArguments = new ArrayList<>();
		Branch entryPointBranch;
		
		List<String> arguments = args == null ? Collections.emptyList() : new ArrayList<>(Arrays.asList(args));
		boolean isHelpPrint = !arguments.isEmpty() && ProcessArgumentsHelper.isHelpPrint(arguments.get(0));
		if(isHelpPrint) arguments.remove(0);
		
		try {
			entryPointBranch = readArguments(arguments, options, entryArguments, !isHelpPrint);
		} catch (ArgumentError e) {
			System.err.println(e.getMessage());
			return;
		}
		
		if(isHelpPrint)
			printHelp(arguments, entryPointBranch);
		else
			runCommand(entryPointBranch.entryPoint, options, entryArguments);
	}
	
	public void run(String args) {
		run(StringUtils.splitWithQuotes(args, " "));
	}

	public static void runHere(String programName, String[] args) {
		new ProcessArguments(programName).addEntryPoints(ReflectUtils.getCallerClass()).run(args);
	}
	
	public static void runHere(String programName, String args) {
		new ProcessArguments(programName).addEntryPoints(ReflectUtils.getCallerClass()).run(args);
	}

	private Branch getEntrylessBranch(String path) throws IllegalArgumentException {
		String[] parts = path.split(" ");
		Branch current = treeRoot;
		int pl = 0;
		
		for(int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if(!ProcessArgumentsHelper.canBeBranchName(p))
				throw new IllegalArgumentException("Name " + p + " cannot be used as a branch path");
			
			if(current.entryPoint != null)
				throw new IllegalArgumentException("Branch '" + path.substring(0, pl) + "' has a declared entry point, it cannot have sub-paths");
			current = current.subBranches.computeIfAbsent(p, _p -> new Branch());
			pl += p.length()+1;
		}
		
		return current;
	}
	
	private OptionsClass getOrCreateOptionClass(Method method) throws NoSuchMethodException, SecurityException {
		if(!ProcessArgumentsHelper.doesMethodUseOptions(method))
			return null;
		Class<?> optionsType = method.getParameterTypes()[0];
		OptionsClass optionsClass = optionClasses.get(optionsType);
		if(optionsClass == null)
			optionsClass = OptionsClass.getOptions(optionsType);
		optionClasses.put(optionsType, optionsClass);
		return optionsClass;
	}
	
	private void printHelp(List<String> args, Branch branch) {
		if(branch.entryPoint != null) {
			EntryPointFunction f = branch.entryPoint;
			Method m = f.method;
			System.out.println("Usage: " + getEntryUsage(f));
			for(int i = f.optionsOffset(); i < m.getParameterCount(); i++) {
				Class<?> argConcreteType = m.getParameterTypes()[i];
				String argName = f.argumentsAnnotations == null ?
						m.getParameters()[i].getName() :
						f.argumentsAnnotations[i].name();
				String argType = argConcreteType.isEnum() ?
						StringUtils.join("|", argConcreteType.getEnumConstants()) :
						argConcreteType.getSimpleName();
				String argDesc = f.argumentsAnnotations == null ?
						"" :
						f.argumentsAnnotations[i].desc();
				if(!argDesc.isBlank())
					argDesc = "  - " + argDesc.replaceAll("\n", "    ");
				System.out.println("  " + argName + " (" + argType + ")" + argDesc);
			}
			if(f.options != null) {
				for(Field optField : new HashSet<>(f.options.optionFields.values())) {
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
			System.out.println(getUnfinishedPathUsage(args, args.size(), branch));
		}
	}
	
	private static void runCommand(EntryPointFunction entry, Map<String, String> options, List<String> argumentsStrings) {
		Object[] arguments = Arrays.copyOf(entry.defaultValues, entry.defaultValues.length);
		
		if(entry.options != null)
			arguments[0] = OptionsHelper.createOptionsInstance(options, entry.options);
		
		boolean validArguments = true;
		
		int entryOptOffset = entry.optionsOffset();
		for(int i = 0; i < argumentsStrings.size(); i++) {
			Parameter p = entry.method.getParameters()[i+entryOptOffset];
			try {
				arguments[i+entryOptOffset] = OptionsHelper.parseOptionValue(
						argumentsStrings.get(i),
						p.getType(),
						entry.argumentsAnnotations[i].name());
			} catch (ArgumentError e) {
				System.err.println(e.getMessage());
				validArguments = false;
			}
		}
		
		if(!validArguments)
			return;
		
		try {
			entry.method.invoke(null, arguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException("Unable to invoke method " + entry.method.getName(), e);
		}
	}
	
	private Branch readArguments(List<String> args, Map<String, String> outOptions,
			List<String> outArguments, boolean checkValidEntryPoint) throws ArgumentError {
		Branch currentBranch = treeRoot;
		
		for(int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			
			if(arg.isBlank())
				throw new IllegalArgumentException("Unexpected blank space");
			
			if(arg.startsWith("-")) {
				args.remove(i);
				if(i == args.size()) {
					outOptions.put(arg, null);
					break;
				}
				String nextArg = args.get(i);
				if(currentBranch.subBranches.containsKey(nextArg)) {
					currentBranch = currentBranch.subBranches.get(nextArg);
					outOptions.put(arg, null);
				} else {
					outOptions.put(arg, nextArg);
					args.remove(i--);
				}
			} else if(currentBranch.entryPoint != null) {
				outArguments.add(arg);
				args.remove(i--);
			} else {
				if(currentBranch.subBranches.containsKey(arg)) {
					currentBranch = currentBranch.subBranches.get(arg);
				} else {
					throw new ArgumentError("Unknown option: " + arg + "\n" 
							+ getUnfinishedPathUsage(args, i, currentBranch));
				}
			}
		}
		
		if(!checkValidEntryPoint)
			return currentBranch;
		
		EntryPointFunction entry = currentBranch.entryPoint;
		
		if(entry == null)
			throw new ArgumentError(getUnfinishedPathUsage(args, args.size(), currentBranch));

		if(outArguments.size() + entry.optionalArgsCount < entry.paramCount() || outArguments.size() > entry.paramCount())
			throw new ArgumentError(getEntryUsage(entry));
		
		if(entry.options == null && !outOptions.isEmpty())
			throw new ArgumentError("Unexpected option(s): " + StringUtils.join(",", outOptions.keySet().toArray()) +
					"\n" + getEntryUsage(entry));
		
		return currentBranch;
	}
	
	private String getCurrentPathString(List<String> args, int readCount) {
		String s = programName;
		for(int i = 0; i < readCount; i++)
			s += " " + args.get(i);
		return s;
	}
	
	private String getUnfinishedPathUsage(List<String> args, int readCount, Branch currentBranch) {
		return "Usage: " + getCurrentPathString(args, readCount) + " " 
				+ StringUtils.join("|", currentBranch.subBranches.keySet()) + " ...";
	}
	
	private String getEntryUsage(EntryPointFunction entry) {
		Method entryMethod = entry.method;
		String usage = "Usage: " + programName;
		if(entry.options != null) {
			for(String opt : entry.options.getAvailableOptionNames())
				usage += " (" + opt + ")";
		}
		usage += " " + entryMethod.getAnnotation(EntryPoint.class).path();
		for(int i = 0; i < entry.paramCount(); i++) {
			if(!entry.argumentsAnnotations[i].defaultValue().isEmpty())
				usage += " [" + entry.argumentsAnnotations[i].name() + "]";
			else
				usage += " <" + entry.argumentsAnnotations[i].name() + ">";
		}
		return usage;
	}
	
}
