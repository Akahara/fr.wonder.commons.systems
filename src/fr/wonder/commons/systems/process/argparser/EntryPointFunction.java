package fr.wonder.commons.systems.process.argparser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.StringUtils;

final class EntryPointFunction {
	
	final Method method;
	final OptionsClass options;
	private final Object[] defaultArgumentValues;
	private final Argument[] argumentsAnnotations;
	private final int optionalArgsCount;
	
	private EntryPointFunction(Method method, OptionsClass options, Argument[] argumentsAnnotations,
			Object[] defaultValues, int optionalArgsCount) {
		this.method = method;
		this.options = options;
		this.argumentsAnnotations = argumentsAnnotations;
		this.defaultArgumentValues = defaultValues;
		this.optionalArgsCount = optionalArgsCount;
	}
	
	static EntryPointFunction createEntryPointFunction(Method method, OptionsClass options) {
		int optionsOffset = options == null ? 0 : 1;
		
		Object[] defaultValues = new Object[method.getParameterCount() - optionsOffset];
		Argument[] argumentsAnnotations = getArgumentAnnotations(method);
		
		if(argumentsAnnotations == null)
			return new EntryPointFunction(method, options, null, defaultValues, 0);
		
		int optIdx = argumentsAnnotations.length;
		while(optIdx-- > 0 && !argumentsAnnotations[optIdx].defaultValue().isEmpty()) {
			Parameter p = method.getParameters()[optIdx+optionsOffset];
			try {
				defaultValues[optIdx] = OptionsHelper.parseOptionValue(
						argumentsAnnotations[optIdx].defaultValue(),
						p.getType(),
						argumentsAnnotations[optIdx].name());
			} catch (ArgumentError e) {
				throw new IllegalStateException("Invalid default value on method " + method, e);
			}
		}
		int optionalArgsCount = method.getParameterCount()-optIdx-optionsOffset-1;
		
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
		if(args != null && m.getParameterCount() != args.length + (ArgParserHelper.doesMethodUseOptions(m) ? 1 : 0))
			throw new IllegalArgumentException("Invalid number of arguments on " + m.getName() + ", either set all arguments or none");
		return args;
	}
	
	public boolean usesOptions() {
		return options != null;
	}
	
	public int paramCount() {
		return method.getParameterCount() - optionsOffset();
	}
	
	public int optionalParamCount() {
		return optionalArgsCount;
	}
	
	public String getParamName(int i) {
		return argumentsAnnotations == null ?
				method.getParameters()[i+optionsOffset()].getName() :
				argumentsAnnotations[i].name();
	}
	
	public Class<?> getParamType(int i) {
		return method.getParameterTypes()[i+optionsOffset()];
	}

	public String getParamDesc(int i) {
		return argumentsAnnotations == null ? "" :
			argumentsAnnotations[i].desc();
	}

	private int optionsOffset() {
		return options == null ? 0 : 1;
	}

	public Object[] finishArgsArray(Object[] rawArguments, Map<String, String> rawOptions, ErrorWrapper errors) throws WrappedException {
		
		for(int i = paramCount() - optionalParamCount(); i < paramCount(); i++) {
			if(rawArguments[i] == null)
				rawArguments[i] = defaultArgumentValues[i];
		}
		if(!usesOptions()) {
			if(!rawOptions.isEmpty())
				errors.addAndThrow("Unexpected options: " + StringUtils.join(", ", rawOptions.keySet()));
			return rawArguments;
		}
		Object[] arguments = new Object[rawArguments.length+1];
		for(int i = 0; i < rawArguments.length; i++)
			arguments[i+1] = rawArguments[i];
		arguments[0] = OptionsHelper.createOptionsInstance(rawOptions, options, errors);
		errors.assertNoErrors();
		return arguments;
	}

}