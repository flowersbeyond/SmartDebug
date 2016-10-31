package cn.edu.thu.tsmart.tool.da.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.edu.thu.tsmart.tool.da.core.validator.TestCase;

public class TestCheckpointCollectingSession {

	// @Test
	// public void findLaunchConfigurationTest1() {
	// // CheckpointCollectingSession.initSingleton(null, null);
	// // CheckpointCollectingSession session =
	// // CheckpointCollectingSession.getSingleton();
	// // session.
	// // ... 构造太麻烦了啊...
	// }

	@Test
	public void testTestCaseAsKeyOfHashMap() {
		TestCase tc1 = new TestCase("clazz", "f");
		TestCase tc2 = new TestCase("clazz", "f");
		TestCase tc3 = new TestCase("claz", "zf");
		TestCase tc4 = new TestCase("clazz", "g");
		TestCase tc5 = new TestCase("jazz", "f");

		Map<TestCase, Integer> fromTestCaseToWhatever = new HashMap<>();
		fromTestCaseToWhatever.put(tc1, 42);
		// manually boxing: avoid ambiguity among overloaded functions
		assertEquals(new Integer(42), fromTestCaseToWhatever.get(tc1));
		assertEquals(new Integer(42), fromTestCaseToWhatever.get(tc2));
		assertEquals(null, fromTestCaseToWhatever.get(tc3));
		assertEquals(null, fromTestCaseToWhatever.get(tc4));
		assertEquals(null, fromTestCaseToWhatever.get(tc5));

		assertEquals(tc1.hashCode(), tc3.hashCode());
		assertNotEquals(tc1, tc3);
	}

}
