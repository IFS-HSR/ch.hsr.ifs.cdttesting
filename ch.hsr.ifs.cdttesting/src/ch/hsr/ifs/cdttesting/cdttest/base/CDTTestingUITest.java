package ch.hsr.ifs.cdttesting.cdttest.base;

import static ch.hsr.ifs.iltis.core.functional.Functional.asOrNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.eclipse.cdt.codan.core.PreferenceConstants;
import org.eclipse.cdt.codan.internal.ui.CodanUIActivator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.cdt.ui.testplugin.Accessor;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.After;
import org.junit.Before;

import ch.hsr.ifs.iltis.core.functional.OptionalUtil;

import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;
import junit.framework.AssertionFailedError;


@SuppressWarnings("restriction")
public abstract class CDTTestingUITest extends CDTTestingTest {

   /**
    * Set this to {@code false} to enforce execution of quickfix and refactoring tests in the editor
    */
   protected boolean executeQuickfixAndRefactoringInEditor = false;

   private static final String INTROVIEW_ID = "org.eclipse.ui.internal.introview";

   @After
   @Override
   public void tearDown() throws Exception {
      closeOpenEditors();
      super.tearDown();
   }

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      //TODO test if this results in a speed-up
      IPreferenceStore store = CodanUIActivator.getDefault().getPreferenceStore(getCurrentProject());
      store.setValue(PreferenceConstants.P_RUN_IN_EDITOR, executeQuickfixAndRefactoringInEditor);
   }

   @Override
   protected void initCurrentExpectedProjectHolders() throws InterruptedException {
      currentProjectHolder = new TestProjectHolder(makeCurrentProjectName(), language, false);
      expectedProjectHolder = new TestProjectHolder(makeExpectedProjectName(), language, true);
      scheduleAndJoinBoth(currentProjectHolder.createProjectAsync(), expectedProjectHolder.createProjectAsync());
   }

   /* -- WORKBENCH -- */

   private static Optional<IWorkbenchPage> getActivePage() {
      return Optional.ofNullable(getActiveWorkbenchWindow().getActivePage());
   }

   protected static IWorkbenchWindow getActiveWorkbenchWindow() {
      IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (activeWorkbenchWindow == null) {
         final IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
         assertEquals("There should be exactly one workbench window. Test will thus fail.", 1, workbenchWindows.length);
         activeWorkbenchWindow = workbenchWindows[0];
      }
      return activeWorkbenchWindow;
   }

   protected static void runEventLoop() {
      while (getActiveWorkbenchWindow().getShell().getDisplay().readAndDispatch()) {
         /* do nothing */
      }
   }

   /* -- EDITORS -- */

   /**
    * Convenience method to get the active editor from the active page
    */
   protected static Optional<AbstractTextEditor> getActiveTextEditor() {
      return getActivePage().map(p -> asOrNull(AbstractTextEditor.class, p));
   }

   /**
    * Convenience method to get the active editor's selection provider
    */
   protected static Optional<ISelectionProvider> getActiveEditorSelectionProvider() {
      return getActiveTextEditor().map(AbstractTextEditor::getSelectionProvider);
   }

   public static void closeWelcomeScreen() throws Exception {
      UIThreadSyncRunnable.run(() -> {
         getActivePage().ifPresent(p -> p.hideView(p.findViewReference(INTROVIEW_ID)));
      });
   }

   protected void closeOpenEditors() throws Exception {
      UIThreadSyncRunnable.run(() -> getActivePage().ifPresent(p -> p.closeAllEditors(false)));
   }

   protected void saveAllEditors() throws Exception {
      UIThreadSyncRunnable.run(() -> {
         getActivePage().ifPresent(p -> p.saveAllEditors(false));
         runEventLoop();
      });
   }

   /**
    * Opens the primary test source file in an editor.
    * 
    * @throws Exception
    */
   protected void openPrimaryTestFileInEditor() throws Exception {
      openTestFileInEditor(getNameOfPrimaryTestFile());
   }

   protected void openTestFileInEditor(final String testSourceFileName) throws Exception {
      if (!testFiles.containsKey(testSourceFileName)) throw new IllegalArgumentException(NLS.bind("No such test file \"{0}\" found.",
            testSourceFileName));
      UIThreadSyncRunnable.run(() -> {
         OptionalUtil.doIfPresentT(getActivePage(), p -> {
            IDE.openEditor(p, getCurrentIFile(testSourceFileName));
            setSelectionInActiveEditorIfAvailable(testFiles.get(testSourceFileName));
            runEventLoop();
         });
      });
   }

   protected void openExternalFileInEditor(final URI absolutePath) throws Exception {
      UIThreadSyncRunnable.run(() -> {
         OptionalUtil.doIfPresentT(getActivePage(), p -> {
            IDE.openEditor(p, new ExternalEditorInput(absolutePath, getCurrentProject()), "org.eclipse.cdt.ui.editor.CEditor", true);
            runEventLoop();
         });
      });
   }

   protected void openExternalFileInEditor(final IPath absolutePath) throws Exception {
      openExternalFileInEditor(URIUtil.toURI(absolutePath));
   }

   private static Optional<TextSelection> getTextSelectionInActiveEditor() {
      return getActiveEditorSelectionProvider().map(ISelectionProvider::getSelection).map(sel -> asOrNull(TextSelection.class, sel));
   }

   private static void setTextSelectionInActiveEditor(ITextSelection selection) {
      getActiveEditorSelectionProvider().orElseThrow(() -> new AssertionError("no active editor found.")).setSelection(selection);
   }

   protected static void setSelectionInActiveEditorIfAvailable(final TestSourceFile testSourceFile) {
      if (testSourceFile != null) testSourceFile.getSelection().ifPresent(sel -> setTextSelectionInActiveEditor(sel));
   }

   private static int getCurrentEditorCaretPosition() {
      return getActiveTextEditor().map(edit -> edit.getAdapter(ITextOperationTarget.class)).map(ot -> JFaceTextUtil.getOffsetForCursorLocation(
            (ITextViewer) ot)).orElse(-1);
   }

   /* -- USER INTERACTION */

   protected void insertUserTyping(final String text, IFile file, int position) throws MalformedTreeException, BadLocationException, IOException {
      position = adaptExpectedOffset(file, position);
      insertUserTypingIntoCurrentProject(text, file, position, 0);
   }

   protected void insertUserTyping(final String text, final IFile file) throws MalformedTreeException, BadLocationException, IOException {
      final Optional<TextSelection> selection = getTextSelectionInActiveEditor();
      if (selection.isPresent()) {
         insertUserTypingIntoCurrentProject(text, file, selection.get().getOffset(), selection.get().getLength());
      } else {
         insertUserTypingIntoCurrentProject(text, file, getCurrentEditorCaretPosition(), 0);
      }
   }

   protected void insertUserTypingIntoCurrentProject(final String text, final IFile file, final int startPosition, final int length)
         throws MalformedTreeException, BadLocationException, IOException {
      new ReplaceEdit(startPosition, length, text.replaceAll("\\n", NL)).apply(getCurrentDocument(file));
   }

   /**
    * This method can e.g. be used to jump to next linked-edit-group by sending
    * c='\t' (tab)
    */
   protected void invokeKeyEvent(final char c) {
      final StyledText textWidget = getActiveTextEditor().map(te -> asOrNull(CEditor.class, te)).orElseThrow(() -> new AssertionFailedError(
            "active editor is no ceditor.")).getViewer().getTextWidget();
      assertNotNull(textWidget);
      final Accessor accessor = new Accessor(textWidget, StyledText.class);
      final Event event = new Event();
      event.character = c;
      event.keyCode = 0;
      event.stateMask = 0;
      accessor.invoke("handleKeyDown", new Object[] { event });
   }

   protected int adaptExpectedOffset(final IFile file, final int offset) throws IOException {
      return adaptExpectedOffset(file.getLocationURI(), offset);
   }

   protected int adaptExpectedOffset(final URI fileLocation, final int offset) throws IOException {
      if (NL.length() < 2) { return offset; }
      final String expectedNewLine = "\n";
      final String expectedSource = getCurrentDocument(fileLocation).get().replace(NL, expectedNewLine);
      return offset + getOffsetAdaptionDelta(offset, expectedSource, expectedNewLine);
   }

   @Deprecated
   protected int adaptActualOffset(final IASTFileLocation fileLocation) throws IOException {
      return adaptActualOffset(getCurrentProject().getFile(fileLocation.getFileName()), fileLocation.getNodeOffset());
   }

   protected int adaptActualOffset(final IFile file, final int offset) throws IOException {
      return adaptActualOffset(file.getLocationURI(), offset);
   }

   protected int adaptActualOffset(final URI fileURI, final int offset) throws IOException {
      if (NL.length() < 2) { return offset; }
      return offset - getOffsetAdaptionDelta(offset, getCurrentDocument(fileURI).get(), NL);
   }

   private int getOffsetAdaptionDelta(final int offset, final String source, final String nl) throws IOException {
      final int amountNewLines = countUpTo(source, nl, offset);
      final int delta = (NL.length() - 1) * amountNewLines;
      return delta;
   }

   protected int adaptActualLength(final IFile file, final int length, final int offset) throws IOException {
      return adaptActualLength(file.getLocationURI(), length, offset);
   }

   protected int adaptActualLength(final URI fileURI, final int length, final int offset) throws IOException {
      if (NL.length() < 2) { return length; }
      return length - getLengthAdaptionDelta(length, offset, getCurrentDocument(fileURI).get(), NL);
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

}
