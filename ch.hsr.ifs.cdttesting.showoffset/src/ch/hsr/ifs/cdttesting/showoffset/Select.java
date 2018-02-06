package ch.hsr.ifs.cdttesting.showoffset;

import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;


public class Select implements InputHandler {

   private IWorkbenchWindow window;

   public void run(final IWorkbenchWindow window) {
      this.window = window;
      SelectionDialog dialog = new SelectionDialog(window.getShell(), this);
      dialog.open();
   }

   @Override
   public void setInput(final int from, final int to) {
      boolean editorAreaVisible = window.getActivePage().isEditorAreaVisible();
      IEditorPart activeEditor = window.getActivePage().getActiveEditor();
      if (editorAreaVisible && activeEditor instanceof ITextEditor) {
         ITextEditor editor = (ITextEditor) activeEditor;
         editor.getSelectionProvider().setSelection(new TextSelection(from, to - from));
      }
   }
}
