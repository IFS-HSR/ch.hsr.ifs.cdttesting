package ch.hsr.ifs.cdttesting;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RtsTest extends SourceFileTest {

	private enum MatcherState {
		skip, inTest, inSource, inExpectedResult
	}

	private static final String testRegexp = "//!(.*)\\s*(\\w*)*$";
	private static final String fileRegexp = "//@(.*)\\s*(\\w*)*$";
	private static final String resultRegexp = "//=.*$";

	protected static Map<String, ArrayList<TestSourceFile>> createTests(BufferedReader inputReader) throws Exception {
		Map<String, ArrayList<TestSourceFile>> testCases = new TreeMap<String, ArrayList<TestSourceFile>>();

		String line;
		ArrayList<TestSourceFile> files = new ArrayList<TestSourceFile>();
		TestSourceFile actFile = null;
		MatcherState matcherState = MatcherState.skip;
		String testName = null;
		boolean bevorFirstTest = true;

		while ((line = inputReader.readLine()) != null) {

			if (lineMatchesBeginOfTest(line)) {
				if (!bevorFirstTest) {
					testCases.put(testName, files);
					files = new ArrayList<TestSourceFile>();
					testName = null;
				}
				matcherState = MatcherState.inTest;
				testName = getNameOfTest(line);
				bevorFirstTest = false;
				continue;
			} else if (lineMatchesBeginOfResult(line)) {
				matcherState = MatcherState.inExpectedResult;
				if (actFile != null) {
					actFile.initExpectedSource();
				}
				continue;
			} else if (lineMatchesFileName(line)) {
				matcherState = MatcherState.inSource;
				actFile = new TestSourceFile(getFileName(line));
				files.add(actFile);
				continue;
			}

			switch (matcherState) {
			case skip:
			case inTest:
				break;
			case inSource:
				if (actFile != null) {
					actFile.addLineToSource(line);
				}
				break;
			case inExpectedResult:
				if (actFile != null) {
					actFile.addLineToExpectedSource(line);
				}
				break;
			}
		}
		testCases.put(testName, files);

		return testCases;
	}

	private static String getFileName(final String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(fileRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return null;
		}
	}

	private static boolean lineMatchesBeginOfTest(final String line) {
		return createMatcherFromString(testRegexp, line).find();
	}

	private static boolean lineMatchesFileName(final String line) {
		return createMatcherFromString(fileRegexp, line).find();
	}

	private static Matcher createMatcherFromString(final String pattern, final String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	private static String getNameOfTest(final String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(testRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return Messages.getString("IncludatorTester.NotNamed");
		}
	}

	private static boolean lineMatchesBeginOfResult(final String line) {
		return createMatcherFromString(resultRegexp, line).find();
	}
}
