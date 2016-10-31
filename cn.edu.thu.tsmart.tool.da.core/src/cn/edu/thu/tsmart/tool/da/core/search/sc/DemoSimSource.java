package cn.edu.thu.tsmart.tool.da.core.search.sc;

public class DemoSimSource {

    public String text = "Hello World!", text2 = ((null));

    private String text3 = "hello Kitty~";

    public int[] f() {
        return new int[0];
    }

    public int[] ia = {42, 233};
    public int[] ib = null;
    public int[] ic = f();
    public int[] id = this.ia;
    public int[] ie = (int[])this.ia;
    @SuppressWarnings("unused")
	public int[] ig = true?ia:ib;

    public void print(int value) {
        System.out.println(value);
    }

    public void input(String value) {
        text2 = value;
    }

    public static void main(String[] args) {
        String s;
        s = "";
        s += ("1" + "2" + "3" + "4");
        System.out.println(s + new DemoSimSource().text3);
    }
}