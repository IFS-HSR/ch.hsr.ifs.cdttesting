package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.cdt.ui.testplugin.Accessor;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsFileInfo;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;
import ch.hsr.ifs.iltis.core.resources.FileUtil;


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
      final RtsFileInfo rtsFileInfo = new RtsFileInfo(getClass(), rtsFileName);
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
      return getCurrentSourceFromRelativePath(activeFileName);
   }

   protected String getCurrentSourceFromRelativePath(final String relativeFilePath) {
      return getFormattedSource(getURI(relativeFilePath, currentProject), currentCproject);
   }

   private URI getURI(final String relativeFilePath, IProject project) {
      return FileUtil.stringToUri(makeProjectAbsolutePath(relativeFilePath, project));
   }

   @Override
   protected String getExpectedSource() {
      return getExpectedSourceFromRelativePath(activeFileName);
   }

   @Override
   protected String getExpectedSourceFromRelativePath(final String relativeFilePath) {
      return getFormattedSource(getURI(relativeFilePath, expectedProject), expectedCproject);
   }

   protected String getFormattedSource(final URI uri, ICProject cProject) {
      return formatDocument(uri, cProject).get();
   }

   protected String getSource(final URI uri) {
      return FileCache.getDocument(uri).get();
   }

   private IDocument formatDocument(final URI uri, ICProject cProject) {
      final IDocument doc = FileCache.getDocument(uri);
      if (cProject instanceof ICProject) {
         final Map<String, Object> options = new HashMap<>(cProject.getOptions(true));
         try {
            final ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(uri, cProject);
            options.put(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT, tu);
            final CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
            final TextEdit te = formatter.format(CodeFormatter.K_TRANSLATION_UNIT, uri.getPath(), 0, doc.getLength(), 0, NL);
            te.apply(doc);
         } catch (CModelException | MalformedTreeException | BadLocationException e) {
            e.printStackTrace();
         }
      }
      return doc;
   }

   protected void executeCommand(final String commandId) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
      final IHandlerService hs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class);
      hs.executeCommand(commandId, null);
   }

   protected void insertUserTyping(final String text, int position) throws MalformedTreeException, BadLocationException, IOException {
      final String path = makeProjectAbsolutePath(activeFileName, currentProject);
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
      final String expectedSource = getTestSourceAbsolutePath(absoluteFilePath, expectedProject).replace(NL, expectedNewLine);
      return offset + getOffsetAdaptionDelta(offset, expectedSource, expectedNewLine);
   }

   protected int adaptExpectedOffsetOfCurrentDocument(final String fileLocation, final int expectedOffset) throws IOException {
      if (NL.length() < 2) { return expectedOffset; }
      final String expectedNewLine = "\n";
      final String expectedSource = getCurrentSourceFromRelativePath(fileLocation).replace(NL, expectedNewLine);
      return expectedOffset + getOffsetAdaptionDelta(expectedOffset, expectedSource, expectedNewLine);
   }

   protected int adaptActualOffset(final IASTFileLocation fileLocation) throws IOException {
      return adaptActualOffset(fileLocation.getFileName(), fileLocation.getNodeOffset());
   }

   protected int adaptActualOffset(final String fileName, final int offset) throws IOException {
      if (NL.length() < 2) { return offset; }
      return offset - getOffsetAdaptionDelta(offset, getCurrentSourceFromRelativePath(fileName), NL);
   }

   private int getOffsetAdaptionDelta(final int offset, final String source, final String nl) throws IOException {
      final int amountNewLines = countUpTo(source, nl, offset);
      final int delta = (NL.length() - 1) * amountNewLines;
      return delta;
   }

   protected Object adaptActualLength(final String fileName, final int length, final int offset) throws IOException {
      if (NL.length() < 2) { return length; }
      return length - getLengthAdaptionDelta(length, offset, getTestSourceAbsolutePath(fileName, currentProject), NL);
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

   private String getTestSourceAbsolutePath(final String absoluteFilePath, IProject proj) throws IOException {
      final IPath projectRelativePath = new Path(absoluteFilePath).makeRelativeTo(proj.getLocation());
      return getTestSource(projectRelativePath.toOSString());
   }

   /**
    * TODO Docu
    * 
    * @param fileName
    * @param failOnProblemNode
    * @param ignoreIncludes
    * @param ignoreComments
    */
   public void fastAssertEquals(final String fileName, EnumSet<ComparisonArg> args) {
      fastAssertEquals(fileName, args, ITranslationUnit.AST_SKIP_ALL_HEADERS);
   }

   // Compares the {@link IASTTranslationUnit} from the code after the QuickFix was
   // applied with the {@link IASTTranslationUnit} from the expected code. To use
   // this method the flag {@code instantiateExpectedProject} has to be set to
   // true.

   /**
    * TODO
    * 
    * @param fileName
    * @param args
    * @param astStyle
    */
   public void fastAssertEquals(final String fileName, EnumSet<ComparisonArg> args, int astStyle) {
      if (args.contains(ComparisonArg.USE_SOURCE_COMPARISON)) {
         assertEqualsWithSource(fileName, args);
      } else {
         assertEqualsWithAST(fileName, args, astStyle);
      }
   }

   private void assertEqualsWithSource(final String fileName, EnumSet<ComparisonArg> args) {
      String expectedSource;
      String currentSource;
      if (args.contains(ComparisonArg.DEBUG_NO_FORMATTING)) {
         expectedSource = getSource(getURI(fileName, expectedProject));
         currentSource = getSource(getURI(fileName, currentProject));
      } else {
         expectedSource = getFormattedSource(getURI(fileName, expectedProject), expectedCproject);
         currentSource = getFormattedSource(getURI(fileName, currentProject), currentCproject);
      }
      if (!args.contains(ComparisonArg.DEBUG_NO_NORMALIZING)) {
         expectedSource = ASTComparison.normalizeCPP(expectedSource);
         currentSource = ASTComparison.normalizeCPP(currentSource);
      }
      assertEquals("Textual comparison", expectedSource, currentSource);
   }

   private void assertEqualsWithAST(final String fileName, EnumSet<ComparisonArg> args, int astStyle) {
      IIndex expectedIndex = null;
      IIndex currentIndex = null;
      try {
         expectedIndex = CCorePlugin.getIndexManager().getIndex(expectedCproject, IIndexManager.ADD_EXTENSION_FRAGMENTS_EDITOR);
         currentIndex = CCorePlugin.getIndexManager().getIndex(currentCproject, IIndexManager.ADD_EXTENSION_FRAGMENTS_EDITOR);
         expectedIndex.acquireReadLock();
         currentIndex.acquireReadLock();
         ASTComparison.assertEqualsAST(getStyledASTFromProject(fileName, expectedCproject, expectedIndex, astStyle), getStyledASTFromProject(fileName,
               currentCproject, currentIndex, astStyle), args);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (expectedIndex != null) {
            expectedIndex.releaseReadLock();
         }
         if (currentIndex != null) {
            currentIndex.releaseReadLock();
         }
      }
   }

   /**
    * Get the expected AST of the active file
    *
    * @author tstauber
    *
    * @return The expected AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getExpectedASTOfActiveFile() {
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
      return getASTFromProject(fileName, expectedProject, expectedCproject);
   }

   /**
    * Get the current AST of the active file
    *
    * @author tstauber
    *
    * @return The current AST or null, if an exception occurred.
    */
   public IASTTranslationUnit getCurrentASTOfActiveFile() {
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
      return getASTFromProject(fileName, currentProject, currentCproject);
   }

   private IASTTranslationUnit getASTFromProject(String fileName, IProject project, ICProject cProject) {
      try {
         return CoreModelUtil.findTranslationUnitForLocation(getURI(fileName, project), cProject).getAST();
      } catch (final CoreException ignored) {
         return null;
      }
   }

   private IASTTranslationUnit getStyledASTFromProject(String fileName, ICProject cProject, IIndex index, int astStyle) {
      try {
         return CoreModelUtil.findTranslationUnitForLocation(getURI(fileName, cProject.getProject()), cProject).getAST(index, astStyle);
      } catch (final CoreException ignored) {
         return null;
      }
   }

}
