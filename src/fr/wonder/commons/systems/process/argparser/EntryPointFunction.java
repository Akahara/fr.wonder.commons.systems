package fr.wonder.commons.systems.process.argparser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

final class EntryPointFunction {
	
	final Method method;
	final OptionsClass options;
	final Argument[] argumentsAnnotations;
	final Object[] defaultValues;
	final int optionalArgsCount;
	
	private EntryPointFunction(Method method, OptionsClass options, Argument[] argumentsAnnotations,
			Object[] defaultValues, int optionalArgsCount) {
		this.method = method;
		this.options = options;
		this.argumentsAnnotations = argumentsAnnotations;
		this.defaultValues = defaultValues;
		this.optionalArgsCount = optionalArgsCount;
	}
	
	static EntryPointFunction getEntryPointFunction(Method method, OptionsClass options) {
		Object[] defaultValues = new Object[method.getParameterCount()];
		Argument[] argumentsAnnotations = getArgumentAnnotations(method);
		
		if(argumentsAnnotations == null)
			return new EntryPointFunction(method, options, null, defaultValues, 0);
		
		int optIdx = argumentsAnnotations.length;
		int optionsOffset = options == null ? 0 : 1;
		while(optIdx-- > 0 && !argumentsAnnotations[optIdx].defaultValue().isEmpty()) {
			Parameter p = method.getParameters()[optIdx+optionsOffset];
			try {
				defaultValues[optIdx+optionsOffset] = OptionsHelper.parseOptionValue(
						argumentsAnnotations[optIdx].defaultValue(),
						p.getType(),
						argumentsAnnotations[optIdx].name());
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Invalid default value on method " + method, e);
			}
		}
		int optionalArgsCount = method.getParameterCount()-optIdx-1-optionsOffset;
		
		return new EntryPointFunction(method, options, argumentsAnnotations, defaultValues, optionalArgsCount);
	}

	private static Argument[] getArgumentAnnotations(Method m) {
		Arguments arguments = m.getAnnotation(Arguments.class);
		Argument argument = m.getAnnotation(Argument.class);
		Argument[] args = null;
		if(arguments != null)
			args = arguments.value();
		else if(argument != null)
			args = new Argument[] { argument };
		if(args != null && m.getParameterCount() != args.length + (ProcessArguments.doesMethodUseOptions(m) ? 1 : 0))
			throw new IllegalArgumentException("Invalid number of arguments on " + m.getName() + ", either set all arguments or none");
		return args;
	}
	
	int paramCount() {
		return method.getParameterCount() - optionsOffset();
	}

	int optionsOffset() {
		return options == null ? 0 : 1;
	}

}