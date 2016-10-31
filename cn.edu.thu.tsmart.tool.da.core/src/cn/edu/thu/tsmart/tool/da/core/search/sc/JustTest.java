package cn.edu.thu.tsmart.tool.da.core.search.sc;

public class JustTest {

	public static int[][] functionReturnsArrayCreation() {
		return new int[][] { { 5, 6, 7 }, { 8, 9, 10 }, { 11, 12, 13 } };
	}
	
	public static void print(double x){
		System.out.println(x);
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	public static void main(String[] args) {
		boolean b = true;
		System.out.println(b ? 123 : "abc");

		//
		int[][] testArrayCreation = new int[][] { { 5, 6, 7 }, { 8, 9, 10 },
				{ 11, 12, 13 } };
		System.out.println(testArrayCreation[1][2]);
		System.out.println(functionReturnsArrayCreation()[1][2]);
		System.out.println(new int[][] { { 5, 6, 7 }, { 8, 9, 10 },
				{ 11, 12, 13 } }[1][2]); // hhh 字面量好

		Class clazz = Integer.class;
		Class[] cs = { Integer.class, Double.class, Boolean.class };

		if (b = false) {
			b = true;
		}
		
		System.out.println((double)5/2); // wow, cast 优先
		System.out.println((double)(5/2));

		System.out.println('c'-1);
		System.out.println((int)'c'-1);
		System.out.println((char)'c'-1);
		System.out.println((char)('c'-1));
		
		double x=3.2;
		char c='c';
		c++;
		print(x++);
		System.out.println(c);
	}

}
