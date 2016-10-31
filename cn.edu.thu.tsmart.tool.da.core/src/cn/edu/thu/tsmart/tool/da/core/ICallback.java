package cn.edu.thu.tsmart.tool.da.core;

/**
 * At first, I wanna use java.lang.Runnable to implement "callback" /
 * "pass a function as argument" in java. <br>
 * Then I learnt that java.lang.Runnable is designed for multithread stuff. So I
 * created this, the ICallback class. <br>
 * TODO use java 8 method references instead?
 * 
 * @author LI Tianchi
 *
 */
public interface ICallback {
	public void run();
}
