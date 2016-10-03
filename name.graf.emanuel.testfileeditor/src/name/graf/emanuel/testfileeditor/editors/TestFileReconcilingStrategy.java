package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.core.runtime.*;

public class TestFileReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension
{
    @SuppressWarnings("unused")
	private IDocument document;
    private TestFileEditor editor;
    
    public TestFileEditor getEditor() {
        return this.editor;
    }
    
    public void setEditor(final TestFileEditor editor) {
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
    
    public void setProgressMonitor(final IProgressMonitor monitor) {
    }
}
