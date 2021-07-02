package fr.wonder.commons.systems.process;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Function;

import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.systems.reflection.ReflectUtils;

public class ManifestUtils {
	
	private static final String SEP = "[ \\-_]";
	/** avariablename */
	public static final Function<String, String> CONVENTION_FLAT_CASE 				= s -> s.toLowerCase().replaceAll(SEP, "");
	/** AVARIABLENAME */
	public static final Function<String, String> CONVENTION_UPPER_FLAT_CASE 		= s -> s.toUpperCase().replaceAll(SEP, "");
	/** a_variable_name */
	public static final Function<String, String> CONVENTION_SNAKE_CASE 				= s -> s.toLowerCase().replaceAll(SEP, "_");
	/** A_VARIABLE_NAME */
	public static final Function<String, String> CONVENTION_SCREAMING_SNAKE_CASE 	= s -> s.toUpperCase().replaceAll(SEP, "_");
	/** a-variable-name */
	public static final Function<String, String> CONVENTION_KEBAB_CASE 				= s -> s.toLowerCase().replaceAll(SEP, "-");
	/** A-VARIABLE-NAME */
	public static final Function<String, String> CONVENTION_SCREAMING_KEBAB_CASE 	= s -> s.toUpperCase().replaceAll(SEP, "-");
	/** aVariableName */
	public static final Function<String, String> CONVENTION_CAMEL_CASE 				= s -> camelCase(s);
	
	private static String camelCase(String s) {
		String[] words = s.split(SEP);
		StringBuilder camel = new StringBuilder(s.length()-words.length+1);
		if(words.length != 0)
			camel.append(words[0].toLowerCase());
		for(int i = 1; i < words.length; i++) {
			String w = words[i];
			if(w.length() > 0) {
				camel.append(Character.toUpperCase(w.charAt(0)));
				for(int j = 1; j < w.length(); j++)
					camel.append(Character.toLowerCase(w.charAt(j)));
			}
		}
		return camel.toString();
	}

	/**
	 * Returns a manifest parsed from the given file.<br>
	 * Each line will be split into a key-value pair, the split position being the
	 * first occurrence of the '=' char. If it does not appear in a line the value
	 * is set to "true" by default.
	 * 
	 * @param file the file to parse
	 * @return the parsed manifest
	 * @throws IOException if the file cannot be read
	 */
	public static Manifest parseManifest(File file) throws IOException {
		Manifest manifest = new Manifest();
		String[] lines = FilesUtils.read(file).split("\n");
		for (String line : lines) {
			if (line.startsWith("#") || line.isBlank())
				continue;
			int split = line.indexOf('=');
			if (split == -1)
				manifest.set(line);
			else
				manifest.set(line.substring(0, split), line.substring(split + 1));
		}
		return manifest;
	}

	/**
	 * Parses a manifest from a single line similar to execution parameters.
	 */
	public static Manifest parseExecutionManifest(String line) {
		Manifest manifest = new Manifest();
		for (String pair : line.split(" ")) {
			if (pair.isBlank())
				continue;
			int split = pair.indexOf(':');
			if (split == -1)
				manifest.set(pair);
			else
				manifest.set(pair.substring(0, split), pair.substring(split + 1));
		}
		return manifest;
	}

	public static <T> T copyManifest(Manifest source, Class<T> manifestClass) {
		return buildManifestFromFields(source, manifestClass, Function.identity());
	}

	/** name conventions are applied to the manifest class fields names */
	public static <T> T buildManifestFromFields(Manifest source, Class<T> manifestClass,
			Function<String, String> nameConvention) {
		try {
			@SuppressWarnings("unchecked")
			T manifest = (T) manifestClass.getConstructors()[0].newInstance();
			for (Field f : manifestClass.getDeclaredFields()) {
				Class<?> fc = f.getType();
				String name = nameConvention.apply(f.getName());
				if (fc == Integer.class || fc == int.class)
					f.setInt(manifest, source.getInt(name));
				else if (fc == Float.class || fc == float.class)
					f.setFloat(manifest, source.getFloat(name));
				else if (fc == Boolean.class || fc == boolean.class)
					f.setBoolean(manifest, source.getBool(name));
				else if (fc == String.class)
					f.set(manifest, source.get(name));
				else if (fc.isEnum())
					f.set(manifest, ReflectUtils.getForName(source.get(name), fc));
				else
					throw new IllegalArgumentException("Unhandled type: " + fc.getSimpleName());
			}
			return manifest;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to create a manifest", e);
		}
	}

	/** name conventions are applied to the manifest values names TODO comment the manifest creation methods */
	public static <T> T buildManifestFromValues(Manifest source, Class<T> manifestClass,
			Function<String, String> nameConvention) throws IllegalArgumentException {
		try {
			@SuppressWarnings("unchecked")
			T manifest = (T) manifestClass.getConstructors()[0].newInstance();

			for (String key : source.getKeys()) {
				Field f;
				try {
					f = manifestClass.getDeclaredField(nameConvention.apply(key));
				} catch (NoSuchFieldException e) {
					throw new IllegalArgumentException(
							"Manifest " + manifestClass.getSimpleName() + " does not have a key named " + key);
				}
				Class<?> fc = f.getType();
				if (fc == Integer.class || fc == int.class)
					f.setInt(manifest, source.getInt(key));
				else if (fc == Float.class || fc == float.class)
					f.setFloat(manifest, source.getFloat(key));
				else if (fc == Boolean.class || fc == boolean.class)
					f.setBoolean(manifest, source.getBool(key));
				else if (fc == String.class)
					f.set(manifest, source.get(key));
				else if (fc.isEnum())
					f.set(manifest, ReflectUtils.getForName(source.get(key), fc));
				else
					throw new IllegalArgumentException("Unhandled type: " + fc.getSimpleName());
			}
			return manifest;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to create a manifest", e);
		}
	}
	
}
