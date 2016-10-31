package cn.edu.thu.tsmart.tool.da.core.search.sc.test;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.edu.thu.tsmart.tool.da.core.search.sc.U;

public class UTest {

	private static void assertEqualsDouble(double exp, double act) {
		assertEquals(exp, act, 1e-6);
	}

	@Test
	public void testMean() {
		assertEqualsDouble(0, U.mean());
		assertEqualsDouble(2.0 / 1, U.mean(2));
		assertEqualsDouble(1.0 / 2, U.mean(0, 1));
		assertEqualsDouble(1.0 / 3, U.mean(0, 1, 0));
		assertEqualsDouble(99.9 / 7,
				U.mean(11.1, 11.1, 22.2, 11.1, 22.2, 11.1, 11.1));

	}

	@Test
	public void testIntSim() {
		assertEqualsDouble(1, U.intSim(233, 233));
		assertEqualsDouble(1, U.intSim(234, 233));
		assertEqualsDouble(1.0 / 2, U.intSim(233, 235));
		assertEqualsDouble(1.0 / 466, U.intSim(233, -233));
		assertEqualsDouble(1.0 / 466, U.intSim(-233, 233));
		assertEqualsDouble(1.0 / 233, U.intSim(233, 0));
		assertEqualsDouble(1.0 / 233, U.intSim(0, 233));
		assertEqualsDouble(1, U.intSim(0, 0));
		assertEqualsDouble(1, U.intSim(-233, -233));
	}

	@Test
	public void testDoubleSim() {
		assertEqualsDouble(1.0, U.doubleSim(11.1, 11.1));
		assertEqualsDouble(1.0, U.doubleSim(11.2, 11.1));
		assertEqualsDouble(1.0, U.doubleSim(12.1, 11.1));
		assertEqualsDouble(1.0 / 2, U.doubleSim(13.1, 11.1));
		assertEqualsDouble(1.0 / 11.1, U.doubleSim(0, 11.1));
		assertEqualsDouble(1.0 / 22.2, U.doubleSim(11.1, -11.1));
		assertEqualsDouble(1.0, U.doubleSim(0, 0));
	}

	@Test
	public void testComputeLevenshteinDistance() {
		assertEquals(0, U.computeLevenshteinDistance("hello", "hello"));
		assertEquals(1, U.computeLevenshteinDistance("Hello", "hello"));
		assertEquals(1, U.computeLevenshteinDistance("ello", "hello"));
		assertEquals(1, U.computeLevenshteinDistance("hello!", "hello"));
		assertEquals(5, U.computeLevenshteinDistance("vwxyz", "hello"));
		assertEquals(3, U.computeLevenshteinDistance("aeuio", "hello"));
		assertEquals(5, U.computeLevenshteinDistance("vwxyz", ""));
		assertEquals(5, U.computeLevenshteinDistance("", "hello"));
		assertEquals(0, U.computeLevenshteinDistance("", ""));
	}

	@Test
	public void testStringSim() {
		assertEqualsDouble(1, U.stringSim("hello", "hello"));
		assertEqualsDouble(1, U.stringSim("Bello", "hello"));
		assertEqualsDouble(1.0 / 5, U.stringSim("vwxyz", "hello"));
		assertEqualsDouble(1.0 / 3, U.stringSim("aeuio", "hello"));
	}

}
