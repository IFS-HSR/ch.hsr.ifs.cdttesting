package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
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

import ch.hsr.ifs.iltis.core.resources.FileUtil;

import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsFileInfo;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;



@RunWith(RtsTestSuite.class)
@SuppressWarnings("restriction")
public class CDTTestingTest extends CDTSourceFileTest {

   public static final String  NL           = System.getProperty("line.separator");
   private static final String INTROVIEW_ID = "org.eclipse.ui.internal.introview";

   public CDTTestingTest() {
      ExternalResourceHelper.copyPluginResourcesToTestingWorkspace(getClass());
   }

   private enum MatcherState {
      skip, inTest, inSource, inExpectedResult
   }

   private static final String testRegex   = "//!(.*)\\s*(\\w*)*$";
   private static final String fileRegex   = "//@(.*)\\s*(\\w*)*$";
   private static final String resultRegex = "//=.*$";

   protected static Map<String, ArrayList<TestSourceFile>> createTests(final BufferedReader inputReader) throws Exception {
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
            throw new Exception("Failed to add referenced project. RTS file " + rtsFileName + " does not contain any test-cases.");
         } else if (testCases.size() > 1) { throw new Exception("RTS files + " + rtsFileName +
                                                                " which represents a referenced project must only contain a single test case."); }
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
   public static Map<String, ArrayList<TestSourceFile>> testCases(final Class<? extends CDTTestingTest> testClass) throws Exception {
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
      FileCache.clean();
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
      FileCache.clean(); // make sure we are not holding any reference to the
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
      openFileInEditor(currentProject.getFile(fileName));
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
            final ExternalEditorInput input = new ExternalEditorInput(FileUtil.stringToUri(absolutePath), currentProject);
            IDE.openEditor(getActivePage(), input, "org.eclipse.cdt.ui.editor.CEditor", true);
            runEventLoop();
         }
      }.runSyncOnUIThread();
   }

   protected IFile getActiveIFile() {
      return getIFile(activeFileName);
   }

   protected IFile getIFile(final String relativePath) {
      return currentProject.getFile(relativePath);
   }

   protected IDocument getActiveDocument() throws Exception {
      return getDocument(getActiveIFile());
   }

   protected IDocument getDocument(final IFile file) {
      return FileCache.getDocument(file);
   }

   protected IDocument getDocument(final String absoluteFilePath) {
      final URI uri = FileUtil.stringToUri(absoluteFilePath);
      return FileCache.getDocument(uri);
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
      final URI uri = FileUtil.stringToUri(absoluteFilePath);

      final IDocument doc = FileCache.getDocument(uri);

      if (expectedCproject instanceof ICProject) {
         final Map<String, Object> options = new HashMap<>(expectedCproject.getOptions(true));

         try {
            final ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(uri, expectedCproject);
            options.put(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT, tu);
            final CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
            final TextEdit te = formatter.format(CodeFormatter.K_TRANSLATION_UNIT, absoluteFilePath, 0, doc.getLength(), 0, NL);
            te.apply(doc);
         } catch (CModelException | MalformedTreeException | BadLocationException e) {
            e.printStackTrace();
         }
      }
      return doc.get();
   }

   protected void executeCommand(final String commandId) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
      final IHandlerService hs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
      hs.executeCommand(commandId, null);
   }

   protected void insertUserTyping(final String text, int position) throws MalformedTreeException, BadLocationException, IOException {
      final String path = makeProjectAbsolutePath(activeFileName);
      position = adaptExpectedOffsetOfCurrentDocument(path, position);
      insertUserTyping(text, position, 0);
   }

   protected void insertUserTyping(final String text) throws MalformedTreeException, BadLocationException, IOException {
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
      if (selectionProvider == null) { return null; }
      final ISelection selection = selectionProvider.getSelection();
      return (selection instanceof TextSelection) ? ((TextSelection) selection) : null;
   }

   private int getCurrentEditorCaretPosition() {
      final ITextViewer viewer = (ITextViewer) getActiveEditor().getAdapter(ITextOperationTarget.class);
      return JFaceTextUtil.getOffsetForCursorLocation(viewer);
   }

   protected void insertUserTyping(final String text, final int startPosition, final int length) throws MalformedTreeException, BadLocationException,
         IOException {
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
      if (NL.length() < 2) { return offset; }
      final String expectedNewLine = "\n";
      final String expectedSource = getTestSourceAbsolutePath(absoluteFilePath).replace(NL, expectedNewLine);
      return offset + getOffsetAdaptionDelta(offset, expectedSource, expectedNewLine);
   }

   protected int adaptExpectedOffsetOfCurrentDocument(final String fileLocation, final int expectedOffset) throws IOException {
      if (NL.length() < 2) { return expectedOffset; }
      final String expectedNewLine = "\n";
      final String expectedSource = getCurrentSourceFromAbsolutePath(fileLocation).replace(NL, expectedNewLine);
      return expectedOffset + getOffsetAdaptionDelta(expectedOffset, expectedSource, expectedNewLine);
   }

   protected int adaptActualOffset(final IASTFileLocation fileLocation) throws IOException {
      return adaptActualOffset(fileLocation.getFileName(), fileLocation.getNodeOffset());
   }

   protected int adaptActualOffset(final String fileName, final int offset) throws IOException {
      if (NL.length() < 2) { return offset; }
      return offset - getOffsetAdaptionDelta(offset, getCurrentSourceFromAbsolutePath(fileName), NL);
   }

   private int getOffsetAdaptionDelta(final int offset, final String source, final String nl) throws IOException {
      final int amountNewLines = countUpTo(source, nl, offset);
      final int delta = (NL.length() - 1) * amountNewLines;
      return delta;
   }

   protected Object adaptActualLength(final String fileName, final int length, final int offset) throws IOException {
      if (NL.length() < 2) { return length; }
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
      final IPath projectRelativePath = new Path(absoluteFilePath).makeRelativeTo(currentProject.getLocation());
      return getTestSource(projectRelativePath.toOSString());
   }

   /**
    * Performs an assertEquals on the passed parameters after using
    * {@link normalize} on them.
    *
    * @author tstauber
    */
   public static void assertEqualsNormalized(final String expected, final String actual) {
      assertEquals(ASTComparison.normalizeCPP(expected), ASTComparison.normalizeCPP(actual));
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
   public void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit currentAST, final boolean failOnProblemNode) {
      if (!instantiateExpectedProject) {
         fail("To use the assertEqualsAST() method, the class must set instantiateExpectedProject=true ");
      }
      ASTComparison.assertEqualsAST(expectedAST, currentAST, failOnProblemNode, false);
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
   public void assertEqualsAST(final IASTTranslationUnit expectedAST, final IASTTranslationUnit currentAST, final boolean failOnProblemNode, final boolean ignoreIncludes) {
      if (!instantiateExpectedProject) {
         fail("To use the assertEqualsAST() method, the class must set instantiateExpectedProject=true ");
      }
      ASTComparison.assertEqualsAST(expectedAST, currentAST, failOnProblemNode, ignoreIncludes);
   }
   
   /**
    * Get the expected AST of the active file
    *
    * @author tstauber
    *
    * @return The expected AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getExpectedAST() {
      return getExpectedAST(activeFileName);
   }
   
   /**
    * Get the expected AST of the file
    *
    * @author tstauber
    *
    * @return The expected AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getExpectedAST(String fileName) {
      final URI expectedURI = FileUtil.stringToUri(makeProjectAbsolutePath(fileName, expectedProject));
      try {
         return CoreModelUtil.findTranslationUnitForLocation(expectedURI, expectedCproject).getAST();
      } catch (final CoreException ignored) {
         return null;
      }
   }

   /**
    * Get the current AST of the active file
    *
    * @author tstauber
    *
    * @return The current AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getCurrentAST() {
      return getCurrentAST(activeFileName);
   }
   
   /**
    * Get the current AST of the file
    *
    * @author tstauber
    *
    * @return The current AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getCurrentAST(String fileName) {
      final URI currentURI = FileUtil.stringToUri(makeProjectAbsolutePath(fileName));
      try {
         return CoreModelUtil.findTranslationUnitForLocation(currentURI, currentCproject).getAST();
      } catch (final CoreException ignored) {
         return null;
      }
   }
   
   

}
