package fr.wonder.commons.tests;

import java.io.File;

import fr.wonder.commons.systems.argparser.ArgParser;
import fr.wonder.commons.systems.argparser.annotations.Argument;
import fr.wonder.commons.systems.argparser.annotations.EntryPoint;
import fr.wonder.commons.systems.argparser.annotations.Option;
import fr.wonder.commons.systems.argparser.annotations.OptionClass;
import fr.wonder.commons.systems.argparser.annotations.ProcessDoc;
import fr.wonder.commons.systems.process.ProcessUtils;
import fr.wonder.commons.systems.reflection.FooBar.EnumFoo;
import fr.wonder.commons.utils.StringUtils;

@ProcessDoc(doc = ""
		+ "----------------------------------\n"
		+ "Documentation example\n"
		+ "\n"
		+ "Will be printed if no arguments\n"
		+ "are provided and the root is not\n"
		+ "an entry point, or --help is used\n"
		+ "on the root branch\n"
		+ "----------------------------------\n")
public class ProcessArguments {

	public static void main(String[] args) {
		testRun("test2 enum E1 e2");
		testRun("test2 enum E1");
		testRun("test2 enum a");
		testRun("test2");
		testRun("test2 file f");
		testRun("");
		testRun("print");
		testRun("print e");
		testRun("print 6");
		testRun("print --abc");
		testRun("print --abc d");
		testRun("print --abc 7");
		testRun("test2 file --bool fileArg");
		testRun("test2 file fileArg");
		testRun("test2 file /a.txt");
		testRun("print --bool --opt E3");
		testRun("? print -b -o E3");
		testRun("print -bdq");
		testRun("print -bd -q");
		testRun("print -bdg valForG");
		testRun("test2 array \"s t \\\\\"r\"");
		testRun("abc \\\" def \"gh ij\" kl");
		testRun("--help");
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
		ProcessUtils.sleep(100); // flushing does not work well in my IDE, waiting makes things a bit better
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
		
		public int unused;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}

	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	@EntryPoint(path = "print")
	public static void a(Options opt, int i) {
		System.out.println("called a with " + i);
		System.out.println(opt);
	}
	
	@Argument(name = "i", desc = "i argument")
	@Argument(name = "j", desc = "j argument")
	@EntryPoint(path = "test")
	public static void b(Options opt, int i, int j) {
		System.out.println("called b with " + i + " " + j);
		System.out.println(opt);
	}

	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "test2 file")
	public static void c(File f) {
		System.out.println("called c with " + f);
	}
	
	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "test2 array")
	public static void d(String f) {
		System.out.println("called d with " + f);
	}
	
	@Argument(name = "f", desc = "enum argument")
	@EntryPoint(path = "test2 enum")
	public static void e(EnumFoo f) {
		System.out.println("called e with " + f);
	}
	
	
}
