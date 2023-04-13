package fr.wonder.commons.tests;

import fr.wonder.commons.systems.argparser.ArgParser;
import fr.wonder.commons.systems.argparser.annotations.Argument;
import fr.wonder.commons.systems.argparser.annotations.EntryPoint;
import fr.wonder.commons.systems.argparser.annotations.InnerOptions;
import fr.wonder.commons.systems.argparser.annotations.Option;
import fr.wonder.commons.systems.argparser.annotations.OptionClass;
import fr.wonder.commons.systems.process.ProcessUtils;
import fr.wonder.commons.systems.reflection.FooBar.EnumFoo;
import fr.wonder.commons.utils.StringUtils;

public class ProcessArguments2 {

	public static void main(String[] args) {
		testRun("3");
		testRun("test2");
		testRun("--help");
		testRun("--suboption 2 .5");
	}
	
	private static void testRun(String args) {
		String[] argArray = StringUtils.splitWithQuotes(args, " ");
		System.out.println("=== '" + args + "' === " + StringUtils.toObjectString(argArray) + " ===\n");
		System.out.flush();
		ArgParser.runHere(argArray);
		System.err.flush();
		System.out.flush();
		System.out.println();
		System.out.flush();
		ProcessUtils.sleep(100);
	}
	
	@OptionClass
	public static class SubOptions {
		@Option(name = "--suboption")
		public int suboption;
	}

	@OptionClass
	public static class UnusedSubOptions {
		@Option(name = "--abc") // collides with Options#val but that does not matter as it is not used
		public int suboption;
	}
	
	@OptionClass
	public static class Options {
		
		@Option(name = "--abc", shorthand = "-a", desc = "The #val option", valueName = "val")
		public int val;
		@Option(name = "--def", shorthand = "-g")
		public String val2;
		@Option(name = "--bool", shorthand = "-b")
		public boolean b;
		@Option(name = "--bool2", shorthand = "-d")
		public boolean d;
		@Option(name = "--bool3", shorthand = "-q")
		public boolean q = true;
		@Option(name = "--opt", desc = "Enum")
		public EnumFoo val4;
		@InnerOptions
		public SubOptions suboptions;
		
		public UnusedSubOptions unusedSuboptions;
		public int unused;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}

	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	@EntryPoint(path = EntryPoint.ROOT_ENTRY_POINT)
	public static void rootEntryPoint(Options opt, float i) {
		System.out.println("called a with " + i);
		System.out.println(opt);
	}
	
}
