package fr.wonder.commons.systems.process.argparser;

import fr.wonder.commons.systems.process.ProcessUtils;
import fr.wonder.commons.systems.reflection.FooBar.EnumFoo;
import fr.wonder.commons.utils.StringUtils;

class PATests2 {

	public static void main(String[] args) {
		testRun("3");
		testRun("test2");
		testRun("--help");
		testRun(".5");
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
	
	public static class Options {
		
		@Option(name = "--abc", shortand = "-a", desc = "The #val option", valueName = "val")
		public int val;
		@Option(name = "--def", shortand = "-g")
		public String val2;
		@Option(name = "--bool", shortand = "-b")
		public boolean b;
		@Option(name = "--bool2", shortand = "-d")
		public boolean d;
		@Option(name = "--bool3", shortand = "-q")
		public boolean q = true;
		@Option(name = "--opt", desc = "Enum")
		public EnumFoo val4;
		
		public int unused;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}

	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	@EntryPoint(path = ":root")
	public static void a(Options opt, float i) {
		System.out.println("called a with " + i);
		System.out.println(opt);
	}
	
}
