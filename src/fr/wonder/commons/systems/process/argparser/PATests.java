package fr.wonder.commons.systems.process.argparser;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import fr.wonder.commons.systems.reflection.FooBar.EnumFoo;
import fr.wonder.commons.utils.StringUtils;

class PATests {

	public static void main(String[] args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		new ProcessArguments().run("process -a 3 test 5 9 -g str".split(" "));
//		new ProcessArguments().run("process test2 file ../a.json".split(" "));
//		new ProcessArguments().run("process test2 array 'long arg'");
		ProcessArguments.runHere("process test 6 4");
	}
	
	public static class Options {
		
		@ProcessOption(name = "--abc", shortand = "-a")
		public int val;
		@ProcessOption(name = "--def", shortand = "-g")
		public String val2;
		@ProcessOption(name = "--bool", shortand = "-b")
		public boolean val3;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}

	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	@EntryPoint(path = "patest print")
	public static void a(Options opt, int i) {
		System.out.println("called a with " + i);
		System.out.println(opt);
	}
	
	@Argument(name = "i", desc = "i argument")
	@Argument(name = "j", desc = "j argument")
	@EntryPoint(path = "patest test")
	public static void b(Options opt, int i, int j) {
		System.out.println("called b with " + i + " " + j);
		System.out.println(opt);
	}

	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "patest test2 file")
	public static void c(File f) {
		System.out.println("called c with " + f);
	}
	
	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "patest test2 array")
	public static void d(String f) {
		System.out.println("called d with " + f);
	}
	
	@Argument(name = "f", desc = "enum argument")
	@EntryPoint(path = "patest test2 enum")
	public static void e(EnumFoo f) {
		System.out.println("called e with " + f);
	}
	
	
}
