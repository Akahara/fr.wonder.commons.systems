package fr.wonder.commons.systems.process;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Manifest {
	
	private final Map<String, String> entries = new HashMap<>();
	
	public Manifest() {
		
	}
	
	public String get(String key) {
		return entries.get(key);
	}
	
	public String get(String key, String defaultValue) {
		String val = entries.get(key);
		return val == null ? defaultValue : val;
	}
	
	public boolean hasKey(String key) {
		return entries.containsKey(key);
	}
	
	public boolean isSetTo(String key, String value) {
		return value.equals(get(key));
	}
	
	public boolean isTrue(String key) {
		return isSetTo(key, "true");
	}
	
	public boolean isFalse(String key) {
		return isSetTo(key, "false");
	}
	
	public boolean getBool(String key) {
		return Boolean.valueOf(entries.get(key));
	}
	
	public int getInt(String key) throws NumberFormatException {
		return Integer.parseInt(key);
	}
	
	public int getIntUnsafe(String key) {
		try {
			return getInt(key);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	public float getFloat(String key) throws NumberFormatException {
		return Float.parseFloat(key);
	}
	
	public float getFloatUnsafe(String key) {
		try {
			return getFloat(key);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	public void set(String key, String value) {
		if(value == null)
			throw new NullPointerException("A manifest value cannot be null");
		entries.put(key, value);
	}
	
	public void set(String key) {
		set(key, "true");
	}
	
	public void set(String key, int value) {
		set(key, Integer.toString(value));
	}
	
	public void set(String key, float value) {
		set(key, Float.toString(value));
	}
	
	public void set(String key, boolean value) {
		set(key, Boolean.toString(value));
	}
	
	public Set<String> getKeys() {
		return entries.keySet();
	}
	
}
