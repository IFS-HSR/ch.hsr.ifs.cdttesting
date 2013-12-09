package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanTest;

public class ExampleCodanTest extends CDTTestingCodanTest {

	@Override
	protected String getProblemId() {
		return MyCodanChecker.MY_PROBLEM_ID;
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		assertProblemMarkers(new String[] { "Declaration 'main' is wrong." });
	}
}
