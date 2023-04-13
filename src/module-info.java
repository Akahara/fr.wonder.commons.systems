module fr.wonder.commons.systems {
	
	exports fr.wonder.commons.systems.process;
	exports fr.wonder.commons.systems.argparser;
	exports fr.wonder.commons.systems.registry;
	exports fr.wonder.commons.systems.reflection;
	exports fr.wonder.commons.systems.debug;
	
	exports fr.wonder.commons.tests;
	opens fr.wonder.commons.tests;
	
	requires transitive fr.wonder.commons;
	
}
