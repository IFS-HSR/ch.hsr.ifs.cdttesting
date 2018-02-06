package ch.hsr.ifs.cdttesting.showoffset;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;


public class ShowOffset {

   public void run() {
      IWorkbenchPage actPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      if (actPage.isEditorAreaVisible() && actPage.getActiveEditor() != null && actPage.getActiveEditor() instanceof ITextEditor) {
         ITextEditor editor = (ITextEditor) actPage.getActiveEditor();
         ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
         Console.print("Selection from offset: " + selection.getOffset() + " with length " + selection.getLength());
      }
   }
}
