package ch.hsr.ifs.cdttesting.cdttest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsFileInfo;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;

@SuppressWarnings("restriction")
@RunWith(RtsTestSuite.class)
public class CDTTestingTest extends CDTSourceFileTest {

	public CDTTestingTest() {
		ExternalResourceHelper.copyPluginResourcesToTestingWorkspace(getClass());
	}

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

	private static String getFileName(String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(fileRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return null;
		}
	}

	private static boolean lineMatchesBeginOfTest(String line) {
		return createMatcherFromString(testRegexp, line).find();
	}

	private static boolean lineMatchesFileName(String line) {
		return createMatcherFromString(fileRegexp, line).find();
	}

	private static Matcher createMatcherFromString(String pattern, String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	private static String getNameOfTest(String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(testRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return "Not Named";
		}
	}

	private static boolean lineMatchesBeginOfResult(String line) {
		return createMatcherFromString(resultRegexp, line).find();
	}

	protected void addReferencedProject(String projectName, String rtsFileName) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(appendSubPackages(rtsFileName));
		try {
			BufferedReader in = rtsFileInfo.getRtsFileReader();
			Map<String, ArrayList<TestSourceFile>> testCases = createTests(in);
			if (testCases.isEmpty()) {
				throw new Exception("Failed to add referenced project. RTS file " + rtsFileName + " does not contain any test-cases.");
			} else if (testCases.size() > 1) {
				throw new Exception("RTS files + " + rtsFileName + " which represents a referenced project must only contain a single test case.");
			}
			referencedProjectsToLoad.put(projectName, testCases.values().iterator().next());
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

	private String appendSubPackages(String rtsFileName) {
		String testClassPackage = getClass().getPackage().getName();
		return testClassPackage + "." + rtsFileName;
	}

	@RTSTestCases
	public static Map<String, ArrayList<TestSourceFile>> testCases(Class<? extends CDTTestingTest> testClass) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(testClass);
		try {
			Map<String, ArrayList<TestSourceFile>> testCases = createTests(rtsFileInfo.getRtsFileReader());
			return testCases;
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	protected void runEventLoop() {
		while (getActiveWorkbenchWindow().getShell().getDisplay().readAndDispatch()) {
			// do nothing
		}
	}

	private IWorkbenchPage getActivePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}

	protected void closeEditorsWithoutSaving() throws Exception {
		FileHelper.clean(); // make sure we are not holding any reference to the open IDocument anymore (otherwise, local changes in dirty editors
							// won't get lost).
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				getActivePage().closeAllEditors(false);
			}
		}.runSyncOnUIThread();
	}

	protected void saveAllEditors() throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				getActivePage().saveAllEditors(false);
				runEventLoop();
			}
		}.runSyncOnUIThread();
	}

	protected void openActiveFileInEditor() throws Exception {
		final IFile file = project.getFile(activeFileName);
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				IDE.openEditor(getActivePage(), file);
				setSelectionIfAvailable(activeFileName);
				runEventLoop();
			}
		}.runSyncOnUIThread();
	}

	protected void setSelectionIfAvailable(String fileName) {
		TestSourceFile file = fileMap.get(fileName);
		if (file != null && file.getSelection() != null) {
			IEditorPart editor = getActivePage().getActiveEditor();
			if (editor instanceof AbstractTextEditor) {
				AbstractTextEditor textEditor = (AbstractTextEditor) editor;
				ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
				selectionProvider.setSelection(file.getSelection());
			}
		}
	}

	protected void openExternalFileInEditor(final String absolutePath) throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				ExternalEditorInput input = new ExternalEditorInput(FileHelper.stringToUri(absolutePath), project);
				IDE.openEditor(getActivePage(), input, "org.eclipse.cdt.ui.editor.CEditor", true);
				runEventLoop();
			}
		}.runSyncOnUIThread();
	}

	protected IFile getActiveIFile() {
		return getIFile(activeFileName);
	}

	protected IFile getIFile(String relativePath) {
		return project.getFile(relativePath);
	}

	protected IDocument getActiveDocument() throws Exception {
		return getDocument(getActiveIFile());
	}

	protected IDocument getDocument(IFile file) {
		return FileHelper.getDocument(file);
	}

	protected String getCurrentSource() {
		return getCurrentSource(activeFileName);
	}

	protected String getCurrentSource(String relativeFilePath) {
		String absolutePath = makeProjectAbsolutePath(relativeFilePath);
		return getCurrentSourceAbsolutePath(absolutePath);
	}

	private String getCurrentSourceAbsolutePath(String absoluteFilePath) {
		return FileHelper.getDocument(FileHelper.stringToUri(absoluteFilePath)).get();
	}

	protected void executeCommand(String commandId) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
		hs.executeCommand(commandId, null);
	}
}
