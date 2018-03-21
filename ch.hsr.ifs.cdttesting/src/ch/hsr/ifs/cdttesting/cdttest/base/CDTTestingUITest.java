package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.util.ExternalEditorInput;
import org.eclipse.cdt.ui.testplugin.Accessor;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
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
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.After;

import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


@SuppressWarnings("restriction")
public abstract class CDTTestingUITest extends CDTTestingTest {

   public static final String  NL           = System.getProperty("line.separator");
   private static final String INTROVIEW_ID = "org.eclipse.ui.internal.introview";

   @After
   @Override
   public void tearDown() throws Exception {
      super.tearDown();
      closeOpenEditors();
   }

   @Override
   protected void initCurrentExpectedProjectHolders() throws InterruptedException {
      currentProjectHolder = new TestProjectHolder(makeCurrentProjectName(), false);
      expectedProjectHolder = new TestProjectHolder(makeExpectedProjectName(), true);
      scheduleAndJoinBoth(currentProjectHolder.createProjectAsync(), expectedProjectHolder.createProjectAsync());
   }

   protected void executeCommand(final String commandId) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IHandlerService.class).executeCommand(commandId, null);
   }

   /* -- WORKBENCH -- */

   private IWorkbenchPage getActivePage() {
      return getActiveWorkbenchWindow().getActivePage();
   }

   protected IWorkbenchWindow getActiveWorkbenchWindow() {
      IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (activeWorkbenchWindow == null) {
         final IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
         assertEquals("There should be exactly one workbench window. Test will thus fail.", 1, workbenchWindows.length);
         activeWorkbenchWindow = workbenchWindows[0];
      }
      return activeWorkbenchWindow;
   }

   protected void runEventLoop() {
      while (getActiveWorkbenchWindow().getShell().getDisplay().readAndDispatch()) {
         /* do nothing */
      }
   }

   /* -- EDITORS -- */

   /**
    * Convenience method to get the active editor from the active page
    */
   protected AbstractTextEditor getActiveEditor() {
      final IEditorPart editor = getActivePage().getActiveEditor();
      return ((editor instanceof AbstractTextEditor) ? ((AbstractTextEditor) editor) : null);
   }

   /**
    * Convenience method to get the active editor's selection provider
    */
   protected ISelectionProvider getActiveEditorSelectionProvider() {
      final AbstractTextEditor editor = getActiveEditor();
      return (editor != null) ? editor.getSelectionProvider() : null;
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

   protected void closeOpenEditors() throws Exception {
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

   /**
    * Opens the primary test source file in an editor.
    * 
    * @throws Exception
    */
   protected void openPrimaryTestFileInEditor() throws Exception {
      openTestFileInEditor(getNameOfPrimaryTestFile());
   }

   protected void openTestFileInEditor(final String testSourceFileName) throws Exception {
      if (!testFiles.containsKey(testSourceFileName)) throw new IllegalArgumentException("No such test file \"" + testSourceFileName + "\" found.");
      new UIThreadSyncRunnable() {

         @Override
         protected void runSave() throws Exception {
            IFile file = getCurrentIFile(testSourceFileName);
            IDE.openEditor(getActivePage(), file);
            setSelectionInActiveEditorIfAvailable(testFiles.get(testSourceFileName));
            runEventLoop();
         }
      }.runSyncOnUIThread();
   }

   protected void openExternalFileInEditor(final URI absolutePath) throws Exception {
      new UIThreadSyncRunnable() {

         @Override
         protected void runSave() throws Exception {
            final ExternalEditorInput input = new ExternalEditorInput(absolutePath, currentProjectHolder.getProject());
            IDE.openEditor(getActivePage(), input, "org.eclipse.cdt.ui.editor.CEditor", true);
            runEventLoop();
         }
      }.runSyncOnUIThread();
   }

   protected void openExternalFileInEditor(final IPath absolutePath) throws Exception {
      openExternalFileInEditor(URIUtil.toURI(absolutePath));
   }

   private TextSelection getTextSelectionInActiveEditor() {
      final ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
      if (selectionProvider == null) { return null; }
      final ISelection selection = selectionProvider.getSelection();
      return (selection instanceof TextSelection) ? ((TextSelection) selection) : null;
   }

   private void setTextSelectionInActiveEditor(TextSelection selection) {
      final ISelectionProvider selectionProvider = getActiveEditorSelectionProvider();
      if (selectionProvider != null) {
         selectionProvider.setSelection(selection);
      } else {
         fail("no active editor found.");
      }
   }

   protected void setSelectionInActiveEditorIfAvailable(final TestSourceFile testSourceFile) {
      if (testSourceFile != null && testSourceFile.getSelection() != null) {
         setTextSelectionInActiveEditor(testSourceFile.getSelection());
      }
   }

   private int getCurrentEditorCaretPosition() {
      final ITextViewer viewer = (ITextViewer) getActiveEditor().getAdapter(ITextOperationTarget.class);
      return JFaceTextUtil.getOffsetForCursorLocation(viewer);
   }

   /* -- USER INTERACTION */

   protected void insertUserTyping(final String text, IFile file, int position) throws MalformedTreeException, BadLocationException, IOException {
      position = adaptExpectedOffset(file, position);
      insertUserTypingIntoCurrentProject(text, file, position, 0);
   }

   protected void insertUserTyping(final String text, final IFile file) throws MalformedTreeException, BadLocationException, IOException {
      final TextSelection selection = getTextSelectionInActiveEditor();
      if (selection != null) {
         insertUserTypingIntoCurrentProject(text, file, selection.getOffset(), selection.getLength());
      } else {
         insertUserTypingIntoCurrentProject(text, file, getCurrentEditorCaretPosition(), 0);
      }
   }

   protected void insertUserTypingIntoCurrentProject(final String text, final IFile file, final int startPosition, final int length)
         throws MalformedTreeException, BadLocationException, IOException {
      new ReplaceEdit(startPosition, length, text.replaceAll("\\n", NL)).apply(currentProjectHolder.getDocument(file));
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
