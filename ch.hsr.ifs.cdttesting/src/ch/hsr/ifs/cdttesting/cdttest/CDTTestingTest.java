package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTProblem;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.cdt.ui.testplugin.Accessor;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import ch.hsr.ifs.cdttesting.cdttest.ASTComparison.ComparisonState;
import ch.hsr.ifs.cdttesting.cdttest.ASTComparison.Pair;
import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsFileInfo;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;
import ch.hsr.ifs.iltis.core.data.AbstractPair;
import ch.hsr.ifs.iltis.core.data.Wrapper;
import ch.hsr.ifs.iltis.core.functional.Functional;

@SuppressWarnings("restriction")
@RunWith(RtsTestSuite.class)
public class CDTTestingTest extends CDTSourceFileTest {

	public static final String NL = System.getProperty("line.separator");
	private static final String INTROVIEW_ID = "org.eclipse.ui.internal.introview";

	public CDTTestingTest() {
		ExternalResourceHelper.copyPluginResourcesToTestingWorkspace(getClass());
	}

	private enum MatcherState {
		skip, inTest, inSource, inExpectedResult
	}

	private static final String testRegex = "//!(.*)\\s*(\\w*)*$";
	private static final String fileRegex = "//@(.*)\\s*(\\w*)*$";
	private static final String resultRegex = "//=.*$";

	protected static Map<String, ArrayList<TestSourceFile>> createTests(final BufferedReader inputReader)
			throws Exception {
		final Map<String, ArrayList<TestSourceFile>> testCases = new TreeMap<>();

		String line;
		ArrayList<TestSourceFile> files = new ArrayList<>();
		TestSourceFile actFile = null;
		MatcherState matcherState = MatcherState.skip;
		String testName = null;
		boolean beforeFirstTest = true;

		while ((line = inputReader.readLine()) != null) {

			if (lineMatchesBeginOfTest(line)) {
				if (!beforeFirstTest) {
					testCases.put(testName, files);
					files = new ArrayList<>();
					testName = null;
				}
				matcherState = MatcherState.inTest;
				testName = getNameOfTest(line);
				beforeFirstTest = false;
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
		final Matcher matcherBeginOfTest = createMatcherFromString(fileRegex, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return null;
		}
	}

	private static boolean lineMatchesBeginOfTest(final String line) {
		return createMatcherFromString(testRegex, line).find();
	}

	private static boolean lineMatchesFileName(final String line) {
		return createMatcherFromString(fileRegex, line).find();
	}

	private static Matcher createMatcherFromString(final String pattern, final String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	private static String getNameOfTest(final String line) {
		final Matcher matcherBeginOfTest = createMatcherFromString(testRegex, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return "Not Named";
		}
	}

	private static boolean lineMatchesBeginOfResult(final String line) {
		return createMatcherFromString(resultRegex, line).find();
	}

	protected void addReferencedProject(final String projectName, final String rtsFileName) throws Exception {
		final RtsFileInfo rtsFileInfo = new RtsFileInfo(appendSubPackages(rtsFileName));
		try {
			final BufferedReader in = rtsFileInfo.getRtsFileReader();
			final Map<String, ArrayList<TestSourceFile>> testCases = createTests(in);
			if (testCases.isEmpty()) {
				throw new Exception("Failed to add referenced project. RTS file " + rtsFileName
						+ " does not contain any test-cases.");
			} else if (testCases.size() > 1) {
				throw new Exception("RTS files + " + rtsFileName
						+ " which represents a referenced project must only contain a single test case.");
			}
			referencedProjectsToLoad.put(projectName, testCases.values().iterator().next());
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

	private String appendSubPackages(final String rtsFileName) {
		final String testClassPackage = getClass().getPackage().getName();
		return testClassPackage + "." + rtsFileName;
	}

	@RTSTestCases
	public static Map<String, ArrayList<TestSourceFile>> testCases(final Class<? extends CDTTestingTest> testClass)
			throws Exception {
		final RtsFileInfo rtsFileInfo = new RtsFileInfo(testClass);
		try {
			final Map<String, ArrayList<TestSourceFile>> testCases = createTests(rtsFileInfo.getRtsFileReader());
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
		FileHelper.clean();
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
		FileHelper.clean(); // make sure we are not holding any reference to the
		// open IDocument anymore (otherwise, local changes
		// in dirty editors
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
		openFileInEditor(activeFileName);
	}

	protected void openFileInEditor(final IFile file) throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				IDE.openEditor(getActivePage(), file);
				setSelectionIfAvailable(file);
				runEventLoop();
			}
		}.runSyncOnUIThread();
	}

	protected void openFileInEditor(final String fileName) throws Exception {
		openFileInEditor(project.getFile(fileName));
	}

	public static void closeWelcomeScreen() throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				final IViewReference viewReference = page.findViewReference(INTROVIEW_ID);
				page.hideView(viewReference);
			}
		}.runSyncOnUIThread();
	}

	protected void setSelectionIfAvailable(final IFile file) {
		final TestSourceFile testSourceFile = fileMap.get(file.getProjectRelativePath().toString());
		if (testSourceFile != null && testSourceFile.getSelection() != null) {
			final ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
			if (selectionProvider != null) {
				selectionProvider.setSelection(testSourceFile.getSelection());
			} else {
				fail("no active editor found.");
			}
		}
	}

	protected AbstractTextEditor getActiveEditor() {
		final IEditorPart editor = getActivePage().getActiveEditor();
		return ((editor instanceof AbstractTextEditor) ? ((AbstractTextEditor) editor) : null);
	}

	protected ISelectionProvider getActiveEditorSelectionProvider() {
		final AbstractTextEditor editor = getActiveEditor();
		return (editor != null) ? editor.getSelectionProvider() : null;
	}

	protected void openExternalFileInEditor(final String absolutePath) throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				final ExternalEditorInput input = new ExternalEditorInput(FileHelper.stringToUri(absolutePath),
						project);
				IDE.openEditor(getActivePage(), input, "org.eclipse.cdt.ui.editor.CEditor", true);
				runEventLoop();
			}
		}.runSyncOnUIThread();
	}

	protected IFile getActiveIFile() {
		return getIFile(activeFileName);
	}

	protected IFile getIFile(final String relativePath) {
		return project.getFile(relativePath);
	}

	protected IDocument getActiveDocument() throws Exception {
		return getDocument(getActiveIFile());
	}

	protected IDocument getDocument(final IFile file) {
		return FileHelper.getDocument(file);
	}

	protected IDocument getDocument(final String absoluteFilePath) {
		final URI uri = FileHelper.stringToUri(absoluteFilePath);
		return FileHelper.getDocument(uri);
	}

	protected String getCurrentSource() {
		return getCurrentSource(activeFileName);
	}

	protected String getCurrentSource(final String relativeFilePath) {
		final String absolutePath = makeProjectAbsolutePath(relativeFilePath);
		return getCurrentSourceFromAbsolutePath(absolutePath);
	}

	protected String getCurrentSourceFromAbsolutePath(final String absoluteFilePath) {
		return getDocument(absoluteFilePath).get();
	}

	@Override
	protected String getExpectedSource() {
		return getExpectedSource(activeFileName);
	}

	@Override
	protected String getExpectedSource(final String relativeFilePath) {
		final String absolutePath = makeProjectAbsolutePath(relativeFilePath, expectedProject);
		return getExpectedSourceFromAbsolutePath(absolutePath);
	}

	protected String getExpectedSourceFromAbsolutePath(final String absoluteFilePath) {
		final URI uri = FileHelper.stringToUri(absoluteFilePath);

		final IDocument doc = FileHelper.getDocument(uri);

		if (expectedCproject instanceof ICProject) {
			final Map<String, Object> options = new HashMap<>(expectedCproject.getOptions(true));

			try {
				final ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(uri, expectedCproject);
				options.put(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT, tu);
				final CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
				final TextEdit te = formatter.format(CodeFormatter.K_TRANSLATION_UNIT, absoluteFilePath, 0,
						doc.getLength(), 0, NL);
				te.apply(doc);
			} catch (CModelException | MalformedTreeException | BadLocationException e) {
				e.printStackTrace();
			}
		}
		return doc.get();
	}

	protected void executeCommand(final String commandId)
			throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		final IHandlerService hs = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getService(IHandlerService.class);
		hs.executeCommand(commandId, null);
	}

	protected void insertUserTyping(final String text, int position)
			throws MalformedTreeException, BadLocationException, IOException {
		final String path = makeProjectAbsolutePath(activeFileName);
		position = adaptExpectedOffsetOfCurrentDocument(path, position);
		insertUserTyping(text, position, 0);
	}

	protected void insertUserTyping(final String text)
			throws MalformedTreeException, BadLocationException, IOException {
		final TextSelection selection = getCurrentEditorTextSelection();
		if (selection != null) {
			insertUserTyping(text, selection.getOffset(), selection.getLength());
			return;
		}
		final int caretPos = getCurrentEditorCaretPosition();
		insertUserTyping(text, caretPos, 0);
	}

	private TextSelection getCurrentEditorTextSelection() {
		final ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
		if (selectionProvider == null) {
			return null;
		}
		final ISelection selection = selectionProvider.getSelection();
		return (selection instanceof TextSelection) ? ((TextSelection) selection) : null;
	}

	private int getCurrentEditorCaretPosition() {
		final ITextViewer viewer = (ITextViewer) getActiveEditor().getAdapter(ITextOperationTarget.class);
		return JFaceTextUtil.getOffsetForCursorLocation(viewer);
	}

	protected void insertUserTyping(final String text, final int startPosition, final int length)
			throws MalformedTreeException, BadLocationException, IOException {
		final IDocument document = getDocument(getActiveIFile());
		new ReplaceEdit(startPosition, length, text.replaceAll("\\n", NL)).apply(document);
	}

	/**
	 * This method can e.g. be used to jump to next linked-edit-group by sending
	 * c='\t' (tab)
	 */
	protected void invokeKeyEvent(final char c) {
		final AbstractTextEditor abstractEditor = getActiveEditor();
		if (!(abstractEditor instanceof CEditor)) {
			fail("active editor is no ceditor.");
		}
		final StyledText textWidget = ((CEditor) abstractEditor).getViewer().getTextWidget();
		assertNotNull(textWidget);
		final Accessor accessor = new Accessor(textWidget, StyledText.class);
		final Event event = new Event();
		event.character = c;
		event.keyCode = 0;
		event.stateMask = 0;
		accessor.invoke("handleKeyDown", new Object[] { event });
	}

	protected int adaptExpectedOffset(final String absoluteFilePath, final int offset) throws IOException {
		if (NL.length() < 2) {
			return offset;
		}
		final String expectedNewLine = "\n";
		final String expectedSource = getTestSourceAbsolutePath(absoluteFilePath).replace(NL, expectedNewLine);
		return offset + getOffsetAdaptionDelta(offset, expectedSource, expectedNewLine);
	}

	protected int adaptExpectedOffsetOfCurrentDocument(final String fileLocation, final int expectedOffset)
			throws IOException {
		if (NL.length() < 2) {
			return expectedOffset;
		}
		final String expectedNewLine = "\n";
		final String expectedSource = getCurrentSourceFromAbsolutePath(fileLocation).replace(NL, expectedNewLine);
		return expectedOffset + getOffsetAdaptionDelta(expectedOffset, expectedSource, expectedNewLine);
	}

	protected int adaptActualOffset(final IASTFileLocation fileLocation) throws IOException {
		return adaptActualOffset(fileLocation.getFileName(), fileLocation.getNodeOffset());
	}

	protected int adaptActualOffset(final String fileName, final int offset) throws IOException {
		if (NL.length() < 2) {
			return offset;
		}
		return offset - getOffsetAdaptionDelta(offset, getCurrentSourceFromAbsolutePath(fileName), NL);
	}

	private int getOffsetAdaptionDelta(final int offset, final String source, final String nl) throws IOException {
		final int amountNewLines = countUpTo(source, nl, offset);
		final int delta = (NL.length() - 1) * amountNewLines;
		return delta;
	}

	protected Object adaptActualLength(final String fileName, final int length, final int offset) throws IOException {
		if (NL.length() < 2) {
			return length;
		}
		return length - getLengthAdaptionDelta(length, offset, getTestSourceAbsolutePath(fileName), NL);
	}

	private int getLengthAdaptionDelta(final int length, final int offset, final String source, final String nl) {
		final int amountNewLines = countFromTo(source, nl, offset, offset + length);
		final int delta = (NL.length() - 1) * amountNewLines;
		return delta;
	}

	private int countFromTo(final String hayStack, final String needle, final int startAt, final int stopAt) {
		int curOffset = startAt;
		int matches = 0;
		while ((curOffset = hayStack.indexOf(needle, curOffset)) < stopAt) {
			if (curOffset == -1) {
				break;
			}
			curOffset += needle.length();
			matches++;
		}
		return matches;
	}

	private int countUpTo(final String hayStack, final String needle, final int stopAt) {
		return countFromTo(hayStack, needle, 0, stopAt);
	}

	private String getTestSourceAbsolutePath(final String absoluteFilePath) throws IOException {
		final IPath projectRelativePath = new Path(absoluteFilePath).makeRelativeTo(project.getLocation());
		return getTestSource(projectRelativePath.toOSString());
	}

	/**
	 * Normalizes the passed {@link String} by removing all testeditor-comments,
	 * removing leading/trailing whitespace and line-breaks, replacing all remaining
	 * line-breaks by ↵ and reducing all groups of whitespace to a single space.
	 *
	 * @author tstauber
	 *
	 * @param in
	 *            The {@link String} that should be normalized.
	 *
	 * @return A normalized copy of the parameter in.
	 **/
	public static String normalize(final String in) {
		// @formatter:off
		return in.replaceAll("/\\*.*\\*/", "") // Remove all test-editor-comments
				.replaceAll("(^((\\r?\\n)|\\s)*|((\\r?\\n)|\\s)*$)", "") // Remove all leading and trailing
																			// linebreaks/whitespace
				.replaceAll("\\s*(\\r?\\n)+\\s*", "↵") // Replace all linebreaks with linebreak-symbol
				.replaceAll("\\s+", " "); // Reduce all groups of whitespace to a single space
		// @formatter:on
	}

	/**
	 * Performs an assertEquals on the passed parameters after using
	 * {@link normalize} on them.
	 *
	 * @author tstauber
	 */
	public static void assertEqualsNormalized(final String expected, final String actual) {
		assertEquals(ASTComparison.normalize(expected), ASTComparison.normalize(actual));
	}

	/**
	 * Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
	 * applied with the {@link IASTTranslationUnit} from the expected code. To use
	 * this method the flag {@code instantiateExpectedProject} has to be set to
	 * true. Fails on occurrence of {@link CPPASTProblem}.
	 *
	 * @author tstauber
	 *
	 */
	public void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit currentAST) {
		assertEqualsAST(expectedAST, currentAST, true);
	}

	/**
	 * Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
	 * applied with the {@link IASTTranslationUnit} from the expected code. To use
	 * this method the flag {@code instantiateExpectedProject} has to be set to
	 * true.
	 *
	 * @author tstauber
	 *
	 */
	public void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit currentAST,
			final boolean failOnProblemNode) {
		if (!instantiateExpectedProject) {
			fail("To use the assertEqualsAST() method, the class must set instantiateExpectedProject=true ");
		}

		ComparisonResult result = equalsIncludes(expectedAST.getIncludeDirectives(), currentAST.getIncludeDirectives(),
				false);

		switch (result.state) {
		case EQUAL:
			assertTrue(true);
			break;
		case ADDITIONAL_INCLUDE:
			assertEqualsWithAttributes("The current AST does have includes in addition to the ones in expected AST.",
					result.attributes);
			break;
		case INCLUDE_ORDER:
			assertEqualsWithAttributes("The order of the includes are not matching.", result.attributes);
			break;
		case MISSING_INCLUDE:
			assertEqualsWithAttributes("The current AST misses some of the includes from the expected AST.",
					result.attributes);
			break;
		default:
			break;
		}

		result = equals(expectedAST, currentAST, failOnProblemNode);

		switch (result.state) {
		case EQUAL:
			assertTrue(true);
			break;
		case DIFFERENT_AMOUNT_OF_CHILDREN:
			assertEqualsWithAttributes("Different amount of children.", result.attributes);
			break;
		case DIFFERENT_TYPE:
			assertEqualsWithAttributes("Different type.", result.attributes);
			break;
		case DIFFERENT_SIGNATURE:
			assertEqualsWithAttributes("Different normalized signatures.", result.attributes);
			break;
		case PROBLEM_NODE:
			assertEqualsWithAttributes("Encountered a IASTProblem node.", result.attributes);
			break;
		default:
			break;
		}
	}

	protected void assertEqualsWithAttributes(String msg, Map<ComparisonAttribute, String> attributes) {
		String lineNo = attributes.get(ComparisonAttribute.LINE_NO);
		String expected = attributes.get(ComparisonAttribute.EXPECTED);
		String actual = attributes.get(ComparisonAttribute.ACTUAL);
		assertEquals(msg + lineNo != null ? " On line no: " + lineNo : "" + " -> ", expected, actual);
	}

	/**
	 * Get the AST of the expected result
	 *
	 * @author tstauber
	 *
	 * @return The expected AST or null, if an exception occurred.
	 */
	public IASTTranslationUnit getExpectedAST() {
		final String absoluteExpectedPath = makeProjectAbsolutePath(activeFileName, expectedProject);
		final URI expectedURI = FileHelper.stringToUri(absoluteExpectedPath);
		try {
			return CoreModelUtil.findTranslationUnitForLocation(expectedURI, expectedCproject).getAST();
		} catch (final CoreException ignored) {
			return null;
		}
	}

	/**
	 * Get the AST of the current result after the quickfix
	 *
	 * @author tstauber
	 *
	 * @return The current AST or null, if an exception occurred.
	 */
	public IASTTranslationUnit getCurrentAST() {
		final String absoluteCurrentPath = makeProjectAbsolutePath(activeFileName);
		final URI currentURI = FileHelper.stringToUri(absoluteCurrentPath);
		try {
			return CoreModelUtil.findTranslationUnitForLocation(currentURI, cproject).getAST();
		} catch (final CoreException ignored) {
			return null;
		}
	}

	protected ComparisonResult equalsIncludes(final IASTPreprocessorIncludeStatement[] expectedStmt,
			final IASTPreprocessorIncludeStatement[] actualStmt, final boolean ignoreOrdering) {
		StringBuffer expectedStr = new StringBuffer();
		StringBuffer actualStr = new StringBuffer();
		if (ignoreOrdering) {
			Set<String> actual = Arrays.stream(actualStmt).map((node) -> node.getRawSignature())
					.collect(Collectors.toSet());
			Set<String> expected = Arrays.stream(expectedStmt).map((node) -> node.getRawSignature())
					.collect(Collectors.toSet());
			if (actual.equals(expected)) {
				return new ComparisonResult(ComparisonState.EQUAL);
			}

			Set<String> onlyInActual = new HashSet<>(actual);
			onlyInActual.removeAll(expected);
			Set<String> onlyInExpected = new HashSet<>(expected);
			onlyInExpected.removeAll(actual);

			final Map<ComparisonAttribute, String> attributes = new HashMap<>();
			attributes.put(ComparisonAttribute.EXPECTED, onlyInExpected.stream().collect(Collectors.joining("\n")));
			attributes.put(ComparisonAttribute.ACTUAL, onlyInActual.stream().collect(Collectors.joining("\n")));
			if (!onlyInActual.isEmpty()) {
				return new ComparisonResult(ComparisonState.ADDITIONAL_INCLUDE, attributes);
			} else {
				return new ComparisonResult(ComparisonState.MISSING_INCLUDE, attributes);
			}

		} else {
			Wrapper<Integer> count = new Wrapper<>(0);
			Functional.zip(expectedStmt, actualStmt)
					.filter((pair) -> !AbstractPair.allElementEquals(pair, this::equalsRaw)).forEachOrdered((pair) -> {
						count.wrapped++;
						expectedStr.append((pair.first() == null ? "---" : pair.first().toString()).concat("\n"));
						actualStr.append((pair.second() == null ? "---" : pair.second().toString()).concat("\n"));
					});
			if (count.wrapped == 0) {
				return new ComparisonResult(ComparisonState.EQUAL);
			}
			final Map<ComparisonAttribute, String> attributes = new HashMap<>();
			attributes.put(ComparisonAttribute.EXPECTED, expectedStr.toString());
			attributes.put(ComparisonAttribute.ACTUAL, actualStr.toString());
			return new ComparisonResult(ComparisonState.INCLUDE_ORDER, attributes);
		}
	}

	private boolean equalsRaw(IASTNode left, IASTNode right) {
		return left.getRawSignature().equals(right.getRawSignature());
	}

	protected ComparisonResult equals(final IASTNode expected, final IASTNode actual, final boolean failOnProblemNode) {
		final IASTNode[] lChilds = expected.getChildren();
		final IASTNode[] rChilds = actual.getChildren();
		final IASTFileLocation fileLocation = actual.getOriginalNode().getFileLocation();
		final String lineNo = fileLocation == null ? "?" : String.valueOf(fileLocation.getStartingLineNumber());
		final Map<ComparisonAttribute, String> attributes = new HashMap<>();
		attributes.put(ComparisonAttribute.EXPECTED, expected.getRawSignature());
		attributes.put(ComparisonAttribute.ACTUAL, actual.getRawSignature());
		attributes.put(ComparisonAttribute.LINE_NO, lineNo);

		if (lChilds.length != rChilds.length) {
			return new ComparisonResult(ComparisonState.DIFFERENT_AMOUNT_OF_CHILDREN, attributes);
		} else if (!expected.getClass().equals(actual.getClass())) {
			return new ComparisonResult(ComparisonState.DIFFERENT_TYPE, attributes);
		} else if (lChilds.length != 0) {
			for (int i = 0; i < lChilds.length; i++) {
				if (lChilds[i] instanceof IASTProblem || rChilds[i] instanceof IASTProblem) {
					return new ComparisonResult(ComparisonState.PROBLEM_NODE, attributes);
				}
				final ComparisonResult childResult = equals(lChilds[i], rChilds[i], failOnProblemNode);
				if (childResult.state != ComparisonState.EQUAL) {
					return childResult;
				}
			}
		} else if (expected instanceof ICPPASTCompoundStatement || expected instanceof ICPPASTInitializerList) {
			return new ComparisonResult(ComparisonState.EQUAL);
		} else if (!normalize(expected.getRawSignature()).equals(normalize(actual.getRawSignature()))) {
			return new ComparisonResult(ComparisonState.DIFFERENT_SIGNATURE, attributes);
		}
		return new ComparisonResult(ComparisonState.EQUAL);
	}

	protected enum ComparisonState {
		DIFFERENT_TYPE, DIFFERENT_AMOUNT_OF_CHILDREN, DIFFERENT_SIGNATURE, EQUAL, PROBLEM_NODE, ADDITIONAL_INCLUDE, MISSING_INCLUDE, INCLUDE_ORDER
	}

	protected enum ComparisonAttribute {
		EXPECTED, ACTUAL, LINE_NO
	}

	protected class ComparisonResult {
		public ComparisonState state;
		public Map<ComparisonAttribute, String> attributes;

		public ComparisonResult(final ComparisonState state, final Map<ComparisonAttribute, String> attributes) {
			this.state = state;
			this.attributes = attributes;
		}

		public ComparisonResult(final ComparisonState state) {
			this.state = state;
			this.attributes = new HashMap<>();
		}
	}
}
