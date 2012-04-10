package ch.hsr.ifs.cdttesting;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(RTSTest.class)
public abstract class JUnit4RtsTest extends SourceFileTest {

	private static final String testRegexp = "//!(.*)\\s*(\\w*)*$";
	private static final String fileRegexp = "//@(.*)\\s*(\\w*)*$";
	private static final String resultRegexp = "//=.*$";

	protected enum MatcherState {
		skip, inTest, inSource, inExpectedResult
	}

	/**
	 * Key: projectName, value: rtsFileName
	 */
	private final LinkedHashMap<String, String> referencedProjectsToLoad;

	public JUnit4RtsTest() {
		referencedProjectsToLoad = new LinkedHashMap<String, String>();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		setUpIndex();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	protected void setUpIndex() throws CoreException, InterruptedException {
		final int tries = 3;
		int attempt = 0;
		boolean status;
		do {
			doSetUpIndex();
			status = checkTestStatus();
		} while (!status && attempt++ < tries);
	}

	private void doSetUpIndex() throws CoreException, InterruptedException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);

		CCorePlugin.getIndexManager().setIndexerId(cproject, IPDOMManager.ID_FAST_INDEXER);
		CCorePlugin.getIndexManager().reindex(cproject);
		for (ICProject curProj : referencedProjects) {
			CCorePlugin.getIndexManager().setIndexerId(curProj, IPDOMManager.ID_FAST_INDEXER);
			CCorePlugin.getIndexManager().reindex(curProj);
		}

		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);

		boolean joined = CCorePlugin.getIndexManager().joinIndexer(20000, NULL_PROGRESS_MONITOR);
		if (!joined) {
			// Second join due to some strange interruption of JobMonitor when starting unit tests.
			System.err.println("First join on indexer failed. Trying again.");
			joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, NULL_PROGRESS_MONITOR);
			assertTrue("The indexing operation of the test CProject has not finished jet. This should not happen...", joined);
		}
	}

	private boolean checkTestStatus() throws CoreException {
		boolean hasFiles = CCorePlugin.getIndexManager().getIndex(cproject).getAllFiles().length != 0;
		if (!hasFiles) {
			System.err.println("Test " + getName() + " is not properly setup and will most likely fail!");
		}
		return hasFiles;
	}

	@RTSTestCases
	public static Map<String, ArrayList<TestSourceFile>> testCases(Class<? extends JUnit4RtsTest> testClass) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(testClass);
		try {
			Map<String, ArrayList<TestSourceFile>> testCases = createTests(rtsFileInfo.getRtsFileReader());
			return testCases;
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

	protected static Map<String, ArrayList<TestSourceFile>> createTests(final BufferedReader inputReader) throws Exception {
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

	protected void addReferencedProject(String projectName, String rtsFileName) {
		referencedProjectsToLoad.put(projectName, appendSubPackages(rtsFileName));
	}

	private String appendSubPackages(String rtsFileName) {
		String testClassPackage = getClass().getPackage().getName();
		return testClassPackage + "." + rtsFileName;
	}

	@Override
	protected void initReferencedProjects() throws Exception {
		for (Entry<String, String> curEntry : referencedProjectsToLoad.entrySet()) {
			initReferencedProject(curEntry.getKey(), curEntry.getValue());
		}
	}

	private void initReferencedProject(String projectName, String rtsFileName) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(rtsFileName);
		try {
			BufferedReader in = rtsFileInfo.getRtsFileReader();
		Map<String, ArrayList<TestSourceFile>> testCases = createTests(in);
		if (testCases.isEmpty()) {
			throw new Exception("Failed ot add referenced project. RTS file " + rtsFileName + " does not contain any test-cases.");
		} else if (testCases.size() > 1) {
			throw new Exception("RTS files + " + rtsFileName + " which represents a referenced project must only contain a single test case.");
		}
		ICProject cProj = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER);
		for (TestSourceFile testFile : testCases.values().iterator().next()) {
			if (testFile.getSource().length() > 0) {
				importFile(testFile.getName(), testFile.getSource(), cProj.getProject());
			}
		}
		referencedProjects.add(cProj);
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}
}
