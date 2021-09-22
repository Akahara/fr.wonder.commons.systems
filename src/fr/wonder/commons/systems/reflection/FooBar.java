package fr.wonder.commons.systems.reflection;

import java.io.Serializable;
import java.util.Arrays;

public class FooBar {

	public static Foo FOO1 = new Foo(1);
	public static Foo FOO2 = new Foo(2);
	public static Foo FOO3 = new Foo(3);
	public static ExtendedFoo EX_FOO1 = new ExtendedFoo(11, .1f);
	public static ExtendedFoo EX_FOO2 = new ExtendedFoo(12, .2f);
	public static Foo EX_FOO3 = new ExtendedFoo(13, .3f);
	public static Foo[] FOOs1 = { FOO1, FOO2, EX_FOO1 };
	public static Foo[] FOOs2 = { FOO1, null, FOO3 };
	public static Bar BAR1 = new Bar(1);
	public static Bar EX_BAR1 = new ExtendedBar(21, "s1");
	public static Bar EX_BAR2 = new ExtendedBar(-22, "s2");
	public static Bar EX_BAR3 = new ExtendedBar(23, null);
	public static Bar[] BARs1 = { BAR1, EX_BAR2 };
	public static Bar[] BARs2 = { BAR1, EX_BAR3, null };
	public static ArrayFoo AR_FOO1 = new ArrayFoo(new int[][] {{ 1, 2 }, { 3, 4 }});
	public static ArrayBar AR_BAR1 = new ArrayBar(new Bar[] { new Bar(4), new Bar(-1), null });
	public static ArrayBar AR_BAR2 = new ArrayBar(new Bar[] { new Bar(4), new ExtendedBar(-1, "s3"), null });
	public static LinkedFoo UNLINKED_FOO = new LinkedFoo(null, -1);
	public static LinkedFoo LINKED_FOO = new LinkedFoo(UNLINKED_FOO, -2);
	public static LinkedFoo SELF_LINKED_FOO = new LinkedFoo(null, -3);
	static { SELF_LINKED_FOO.next = SELF_LINKED_FOO; }
	public static LinkedFoo PAIRED_FOO1 = new LinkedFoo(null, -4);
	public static LinkedFoo PAIRED_FOO2 = new LinkedFoo(PAIRED_FOO1, -5);
	static { PAIRED_FOO1.next = PAIRED_FOO2; }
	
	public static class Foo implements Serializable {
		private static final long serialVersionUID = 6704406218884985517L;
		
		public int i;
		
		public Foo(int i) {
			this.i = i;
		}
		
		public Foo() {
			this(0);
		}
		
		@Override
		public String toString() {
			return "foo("+i+")";
		}
		
	}
	
	public static class ExtendedFoo extends Foo {
		private static final long serialVersionUID = -2332161542968304051L;
		
		public float f;
		
		public ExtendedFoo(int i, float f) {
			super(i);
			this.f = f;
		}
		
		public ExtendedFoo() {
		}
		
		@Override
		public String toString() {
			return "exfoo("+i+","+f+")";
		}
	}
	
	public static class Bar implements Serializable {
		private static final long serialVersionUID = 6840548448567957510L;
		
		public int b;

		public Bar(int b) {
			this.b = b;
		}
		
		public Bar() {
			this(0);
		}
		
		@Override
		public String toString() {
			return "bar("+b+")";
		}
	}
	
	public static class ExtendedBar extends Bar {
		private static final long serialVersionUID = 7662751801054236089L;
		
		public String s;
		
		public ExtendedBar(int b, String s) {
			super(b);
			this.s = s;
		}
		
		public ExtendedBar() {
			
		}
		
		@Override
		public String toString() {
			return "exbar("+b+","+s+")";
		}
		
	}
	
	public static class ArrayFoo implements Serializable {
		private static final long serialVersionUID = -7590266738871446722L;
		
		public int[][] array;
		
		public ArrayFoo(int[][] array) {
			this.array = array;
		}
		
		public ArrayFoo() {
			
		}
		
		@Override
		public String toString() {
			return "arrayfoo("+Arrays.deepToString(array)+")";
		}
		
	}
	
	public static class ArrayBar implements Serializable {
		private static final long serialVersionUID = 7442600819466164067L;
		
		public Bar[] bars;
		
		public ArrayBar(Bar[] bars) {
			this.bars = bars;
		}
		
		public ArrayBar() {
			
		}
		
		@Override
		public String toString() {
			return "arraybar("+Arrays.deepToString(bars)+")";
		}
		
	}
	
	public static class LinkedFoo implements Serializable {
		private static final long serialVersionUID = -1363150136220575086L;
		
		LinkedFoo next;
		int self;
		
		LinkedFoo(LinkedFoo next, int self) {
			this.next = next;
			this.self = self;
		}
		
	}
	
	public static enum EnumFoo {
		
		E1,
		E2,
		E3;
		
	}
	
	public static enum EnumBar {
		
		B1(1, 2f, "b1"),
		B2(2, 4f, "b2"),
		B3(3, 6f, "b3");
		
		int i;
		float f;
		String s;
		
		private EnumBar(int i, float f, String s) {
			this.i = i;
			this.f = f;
			this.s = s;
		}
		
	}
	
}
