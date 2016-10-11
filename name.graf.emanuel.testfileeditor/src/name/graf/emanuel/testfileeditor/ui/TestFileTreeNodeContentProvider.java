package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import name.graf.emanuel.testfileeditor.Activator;

public class TestFileTreeNodeContentProvider extends TreeNodeContentProvider {
    protected IPositionUpdater fPositionUpdater;
    private final IDocumentProvider provider;
    private TestFile file;
    private final IEditorInput input;

    public TestFileTreeNodeContentProvider(final IDocumentProvider provider, final IEditorInput input) {
        super();
        this.fPositionUpdater = new DefaultPositionUpdater(Activator.TEST_FILE_PARTITIONING);
        this.provider = provider;
        this.input = input;
        this.file = new TestFile(input.getName());
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        if (oldInput != null) {
            final IDocument document = this.provider.getDocument(oldInput);
            if (document != null) {
                document.removePositionUpdater(this.fPositionUpdater);
            }
        }

        if (newInput != null) {
            final IDocument document = this.provider.getDocument(newInput);
            if (document != null) {
                document.addPositionUpdater(this.fPositionUpdater);
                this.file.setDocument(document);
            }
        }
    }

    @Override
    public void dispose() {
        if (this.file != null) {
            this.file = null;
        }
    }

    public boolean isDeleted(final Object element) {
        return false;
    }

    @Override
    public Object[] getElements(final Object element) {
        return new Object[] { this.file };
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode) element;
            return node.hasChildren();
        }
        final boolean ret = element == this.file || element == this.input;
        return ret;
    }

    @Override
    public Object getParent(final Object element) {
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode) element;
            return node.getParent();
        }
        if (element == this.file) {
            return this.input;
        }
        return null;
    }

    @Override
    public Object[] getChildren(final Object element) {
        if (element == this.file) {
            final Test[] tests = this.file.getTests();
            return tests;
        }
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode) element;
            return node.getChildren();
        }
        return new Object[0];
    }
}
