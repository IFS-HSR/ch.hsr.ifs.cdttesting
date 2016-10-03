package name.graf.emanuel.testfileeditor.ui;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.editors.TestFileEditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class TestFileContentOutlinePage extends ContentOutlinePage
{
    private final IDocumentProvider provider;
    private final TestFileEditor editor;
    private IEditorInput input;
    private InteliTreeViewer myTreeViewer;
    
    public TestFileContentOutlinePage(final IDocumentProvider documentProvider, final TestFileEditor editor) {
        super();
        this.provider = documentProvider;
        this.editor = editor;
    }
    
    public void setInput(final IEditorInput input) {
        this.input = input;
        this.update();
    }
    
    public void update() {
        new UIJob("UpdateOutline") {
            @Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
                final InteliTreeViewer viewer = (InteliTreeViewer)TestFileContentOutlinePage.this.getTreeViewer();
                if (viewer != null) {
                    viewer.saveExpandedState();
                    final Control control = viewer.getControl();
                    if (control != null && !control.isDisposed()) {
                        control.setRedraw(false);
                        viewer.setInput(TestFileContentOutlinePage.this.input);
                        viewer.loadExpandedState();
                        control.setRedraw(true);
                    }
                }
                return new Status(0, Activator.PLUGIN_ID, 0, "ok", (Throwable)null);
            }
        }.schedule();
    }
    
    @Override
	public void selectionChanged(final SelectionChangedEvent event) {
        super.selectionChanged(event);
        final ISelection selection = event.getSelection();
        if (selection.isEmpty()) {
            this.editor.resetHighlightRange();
        }
        else if (((IStructuredSelection)selection).getFirstElement() instanceof ITestFileNode) {
            final ITestFileNode segment = (ITestFileNode)((IStructuredSelection)selection).getFirstElement();
            final int start = segment.getPosition().getOffset();
            final int length = segment.getPosition().getLength();
            try {
                this.editor.setHighlightRange(start, length, true);
            }
            catch (IllegalArgumentException ex) {
                this.editor.resetHighlightRange();
            }
        }
        else {
            this.editor.resetHighlightRange();
        }
    }
    
    @Override
	protected TreeViewer getTreeViewer() {
        return this.myTreeViewer;
    }
    
    @Override
	public Control getControl() {
        if (this.myTreeViewer == null) {
            return null;
        }
        return this.myTreeViewer.getControl();
    }
    
    @Override
	public ISelection getSelection() {
        if (this.myTreeViewer == null) {
            return StructuredSelection.EMPTY;
        }
        return this.myTreeViewer.getSelection();
    }
    
    @Override
	public void setFocus() {
        this.myTreeViewer.getControl().setFocus();
    }
    
    @Override
	public void setSelection(final ISelection selection) {
        if (this.myTreeViewer != null) {
            this.myTreeViewer.setSelection(selection);
        }
    }
    
    @Override
	public void createControl(final Composite parent) {
        (this.myTreeViewer = new InteliTreeViewer(parent, 770)).addSelectionChangedListener(this);
        final TreeViewer viewer = this.getTreeViewer();
        viewer.setContentProvider(new TestFileTreeNodeContentProvider(this.provider, this.input));
        viewer.setLabelProvider(new TestFileLabelProvider());
        viewer.addSelectionChangedListener(this);
        if (this.input != null) {
            viewer.setInput(this.input);
        }
		viewer.expandAll();
    }
}
