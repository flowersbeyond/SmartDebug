package cn.edu.thu.tsmart.tool.da.core.search.sc;

import java.util.ArrayList;
import java.util.List;

public class Apple {
	
	public static double f(int x){
		return Math.sqrt(x);
	}
	
	public static void main(String[] args) {
		int [] ia={3,4,5};
		int[][] iaa = { { 5, 6, 7 }, { 8, 9, 10 },
				{ 11, 12, 13 } };
		List<String> al = null;
		int j = 0;
		System.out.println(j);
		System.out.println(al);
		j = (int) 8.0;
		j = (int) f(j);
		al=new ArrayList<String>();
		
		System.out.println(ia[2]);
		System.out.println(iaa[2][2]);
	}
}