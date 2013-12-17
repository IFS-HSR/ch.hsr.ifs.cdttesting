package ch.hsr.ifs.cdttesting.cdttest;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.cdt.ui.testplugin.Accessor;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
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

	public static final String NL = System.getProperty("line.separator");

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
			ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
			if (selectionProvider != null) {
				selectionProvider.setSelection(file.getSelection());
			} else {
				fail("no active editor found.");
			}
		}
	}

	protected AbstractTextEditor getActiveEditor() {
		IEditorPart editor = getActivePage().getActiveEditor();
		return ((editor instanceof AbstractTextEditor) ? ((AbstractTextEditor) editor) : null);
	}

	protected ISelectionProvider getActiveEditorSelectionProvider() {
		AbstractTextEditor editor = getActiveEditor();
		return (editor != null) ? editor.getSelectionProvider() : null;
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

	protected void insertUserTyping(String text, int position) throws MalformedTreeException, BadLocationException {
		insertUserTyping(text, position, 0);
	}

	protected void insertUserTyping(String text) throws MalformedTreeException, BadLocationException {
		TextSelection selection = getCurrentEditorTextSelection();
		if (selection != null) {
			insertUserTyping(text, selection.getOffset(), selection.getLength());
			return;
		}
		int caretPos = getCurrentEditorCaretPosition();
		insertUserTyping(text, caretPos, 0);
	}

	private TextSelection getCurrentEditorTextSelection() {
		ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
		if (selectionProvider == null) {
			return null;
		}
		ISelection selection = selectionProvider.getSelection();
		return (selection instanceof TextSelection) ? ((TextSelection) selection) : null;
	}

	private int getCurrentEditorCaretPosition() {
		ITextViewer viewer = (ITextViewer) getActiveEditor().getAdapter(ITextOperationTarget.class);
		return JFaceTextUtil.getOffsetForCursorLocation(viewer);
	}

	protected void insertUserTyping(String text, int startPosition, int length) throws MalformedTreeException, BadLocationException {
		IDocument document = getDocument(getActiveIFile());
		//TODO: should adapt position so this also works on windows (extract from includator testing-infrastructure)
		new ReplaceEdit(startPosition, length, text.replaceAll("\\n", NL)).apply(document);
	}

	/**
	 * This method can e.g. be used to jump to next linked-edit-group by sending c='\t' (tab)
	 */
	protected void invokeKeyEvent(char c) {
		AbstractTextEditor abstractEditor = getActiveEditor();
		if (!(abstractEditor instanceof CEditor)) {
			fail("active editor is no ceditor.");
		}
		StyledText textWidget = ((CEditor) abstractEditor).getViewer().getTextWidget();
		assertNotNull(textWidget);
		Accessor accessor = new Accessor(textWidget, StyledText.class);
		Event event = new Event();
		event.character = c;
		event.keyCode = 0;
		event.stateMask = 0;
		accessor.invoke("handleKeyDown", new Object[] { event });
	}
}
