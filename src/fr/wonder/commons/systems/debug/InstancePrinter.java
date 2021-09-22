package fr.wonder.commons.systems.debug;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.commons.systems.reflection.PrimitiveUtils;
import fr.wonder.commons.systems.reflection.ReflectUtils;

public class InstancePrinter {
	
	// TODO comment InstancePrinter
	// TODO add workarounds for Maps, Lists...
	
	public static void dump(Object o) {
		new InstancePrinter().dumpObject(o);
	}
	
	private final Map<Object, Integer> referenceCounts = new IdentityHashMap<>();
	
	@SuppressWarnings("serial")
	private final List<Object> referencesIndices = new ArrayList<>() {
		public int indexOf(Object o) {
			for(int i = 0; i < size(); i++)
				if(get(i) == o)
					return i;
			return -1;
		};
	};
	
	private boolean[] visitedObjects;
	
	public void dumpObject(Object o) {
		referenceCounts.clear();
		collectReferencesCount(o);
		visitedObjects = new boolean[referenceCounts.size()];
		
		System.out.println(printValue(o));
	}
	
	private void collectReferencesCount(Object o) {
		if(o == null)
			return;
		Class<?> otype = o.getClass();
		if(!isComplexPrintable(otype))
			return;
		
		Integer rc = referenceCounts.get(o);
		if(rc == null) {
			referencesIndices.add(o);
			referenceCounts.put(o, 1);
		} else {
			referenceCounts.put(o, rc+1);
			return;
		}
		
		if(otype.isArray()) {
			int l = Array.getLength(o);
			for(int i = 0; i < l; i++)
				collectReferencesCount(Array.get(o, i));
		} else {
			ReflectUtils.runOnInstanceFields(o, (field, value) -> {
				collectReferencesCount(value);
			}, true);
		}
	}
	
	private boolean isComplexPrintable(Class<?> type) {
		return !PrimitiveUtils.isPrimitiveType(type) && type != String.class && !type.isEnum();
	}
	
	private String printValue(Object o) {
		if(o == null)
			return "null";
		
		Class<?> type = o.getClass();
		
		if(type == String.class)
			return '"' + o.toString() + '"';
		if(!isComplexPrintable(type))
			return o.toString();
		
		StringBuilder sb = new StringBuilder();
		int oiindex = referencesIndices.indexOf(o);
		if(!visitedObjects[oiindex]) {
			visitedObjects[oiindex] = true;
		} else {
			return "(->" + oiindex +")";
		}
		
		if(type.isArray()) {
			if(!isComplexPrintable(type.componentType()) && Array.getLength(o) < 10)
				return simpleArrayString(o);
			int l = Array.getLength(o);
			if(l != 0) {
				sb.append("[\n");
				for(int i = 0; i < l; i++) {
					sb.append(printValue(Array.get(o, i)).indent(2));
					sb.setLength(sb.length()-1); // remove the '\n' added by #indent
					sb.append(",\n");
				}
				sb.replace(sb.length()-2, sb.length(), "\n]");
			} else {
				sb.append("[]");
			}
			return sb.toString();
		}
		
		sb.append(type.getSimpleName());
		sb.append(' ');
		if(referenceCounts.get(o) > 1)
			sb.append("(id=" + oiindex + ") ");
		int objBegin = sb.length();
		if(ReflectUtils.runOnInstanceFields(o, (field, value) -> {
			int sbl = sb.length();
			if(!field.canAccess(o))
				sb.append("  -inaccessible-\n");
			else
				sb.append(printValue(value).indent(2));
			sb.replace(sbl, sbl+1, "  " + field.getName() + ":");
			sb.insert(sb.length()-1, ',');
		}, true)) {
			sb.insert(objBegin, "{\n");
			sb.replace(sb.length()-2, sb.length(), "\n}");
		} else {
			sb.append("{}");
		}
		return sb.toString();
	}
	
	private String simpleArrayString(Object o) {
		int l = Array.getLength(o);
		if(l == 0)
			return "[]";
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for(int i = 0; i < l; i++) {
			sb.append(Array.get(o, i));
			sb.append(", ");
		}
		sb.setLength(sb.length()-2);
		sb.append(" ]");
		return sb.toString();
	}
	
}
