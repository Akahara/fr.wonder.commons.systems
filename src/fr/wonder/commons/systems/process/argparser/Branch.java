package fr.wonder.commons.systems.process.argparser;

import java.util.HashMap;
import java.util.Map;

final class Branch {
	
	final Map<String, Branch> subBranches = new HashMap<>(0);
	EntryPointFunction entryPoint = null;
	
}