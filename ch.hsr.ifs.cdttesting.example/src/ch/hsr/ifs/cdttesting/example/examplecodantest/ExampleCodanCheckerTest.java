package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanCheckerTest;

public class ExampleCodanCheckerTest extends CDTTestingCodanCheckerTest {

	@Override
	protected String getProblemId() {
		return MyCodanChecker.MY_PROBLEM_ID;
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		int markerExpectedOnLine = 1;
		assertMarkerPositions(markerExpectedOnLine);
		assertProblemMarkerMessages(new String[] { "Declaration 'main' is wrong." });
	}
}
