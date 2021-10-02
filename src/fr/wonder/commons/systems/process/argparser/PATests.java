package fr.wonder.commons.systems.process.argparser;

import java.io.File;

import fr.wonder.commons.systems.reflection.FooBar.EnumFoo;
import fr.wonder.commons.utils.StringUtils;

class PATests {

	public static void main(String[] args) {
		ProcessArguments.runHere("patest", "test2 enum E1 -opt");
	}
	
	public static class Options {
		
		@ProcessOption(name = "--abc", shortand = "-a", desc = "The #val option")
		public int val;
		@ProcessOption(name = "--def", shortand = "-g")
		public String val2;
		@ProcessOption(name = "--bool", shortand = "-b")
		public boolean val3;
		@ProcessOption(name = "--opt", desc = "Enum")
		public EnumFoo val4;
		
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
