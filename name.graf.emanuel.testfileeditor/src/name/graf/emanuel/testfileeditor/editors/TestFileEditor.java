package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.ui.editors.text.*;
import name.graf.emanuel.testfileeditor.ui.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.projection.*;
import java.util.*;
import org.eclipse.ui.views.contentoutline.*;

public class TestFileEditor extends TextEditor
{
    private ColorManager colorManager;
    private TestFileContentOutlinePage fOutlinePage;
    private ProjectionSupport projectionSupport;
    private ProjectionAnnotationModel annotationModel;
    private Annotation[] oldAnnotations;
    
    public TestFileEditor() {
        super();
        this.colorManager = new ColorManager();
        this.setSourceViewerConfiguration((SourceViewerConfiguration)new TestFileConfiguration(this.colorManager, this));
        this.setDocumentProvider((IDocumentProvider)new TestFileDocumentProvider());
    }
    
    public void dispose() {
        this.colorManager.dispose();
        super.dispose();
    }
    
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);
        final ProjectionViewer viewer = (ProjectionViewer)this.getSourceViewer();
        this.projectionSupport = new ProjectionSupport(viewer, this.getAnnotationAccess(), this.getSharedColors());
        this.projectionSupport.install();
        viewer.doOperation(19);
        this.annotationModel = viewer.getProjectionAnnotationModel();
    }
    
    protected ISourceViewer createSourceViewer(final Composite parent, final IVerticalRuler ruler, final int styles) {
        final ISourceViewer viewer = (ISourceViewer)new ProjectionViewer(parent, ruler, this.getOverviewRuler(), this.isOverviewRulerVisible(), styles);
        this.getSourceViewerDecorationSupport(viewer);
        return viewer;
    }
    
    public void updateFoldingStructure(final Vector<Position> positions) {
        final Annotation[] annotations = new Annotation[positions.size()];
        final HashMap<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
        for (int i = 0; i < positions.size(); ++i) {
            final ProjectionAnnotation annotation = new ProjectionAnnotation();
            newAnnotations.put((Annotation)annotation, positions.get(i));
            annotations[i] = (Annotation)annotation;
        }
        this.annotationModel.modifyAnnotations(this.oldAnnotations, newAnnotations, (Annotation[])null);
        this.oldAnnotations = annotations;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public <T> T getAdapter(final Class<T> adapter) {
        if (IContentOutlinePage.class.equals(adapter)) {
            if (this.fOutlinePage == null) {
                this.fOutlinePage = new TestFileContentOutlinePage(this.getDocumentProvider(), this);
                if (this.getEditorInput() == null) {}
                this.fOutlinePage.setInput(this.getEditorInput());
            }
            return adapter.isInstance(this.fOutlinePage) ? (T)this.fOutlinePage : null;
        }
        return super.getAdapter(adapter);
    }
    
    protected void editorSaved() {
        if (this.fOutlinePage != null) {
            this.fOutlinePage.update();
        }
        super.editorSaved();
    }
    
    public TestFileContentOutlinePage getOutline() {
        return this.fOutlinePage;
    }
    
    protected void handleCursorPositionChanged() {
        super.handleCursorPositionChanged();
    }
}
