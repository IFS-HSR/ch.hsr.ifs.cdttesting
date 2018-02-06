package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

import name.graf.emanuel.testfileeditor.ui.Editor;


public class ReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

   @SuppressWarnings("unused")
   private IDocument document;
   private Editor    editor;

   public Editor getEditor() {
      return this.editor;
   }

   public void setEditor(final Editor editor) {
      this.editor = editor;
   }

   public void reconcile(final IRegion partition) {
      this.initialReconcile();
   }

   public void reconcile(final DirtyRegion dirtyRegion, final IRegion subRegion) {
      this.initialReconcile();
   }

   public void setDocument(final IDocument document) {
      this.document = document;
   }

   public void initialReconcile() {
      if (this.editor.getOutline() != null) {
         this.editor.getOutline().update();
      }
   }

   public void setProgressMonitor(final IProgressMonitor monitor) {}
}
