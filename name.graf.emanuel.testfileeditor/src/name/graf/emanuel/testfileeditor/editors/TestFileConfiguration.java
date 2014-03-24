package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.source.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.reconciler.*;

public class TestFileConfiguration extends SourceViewerConfiguration
{
    private TestFileDoubleClickStrategy doubleClickStrategy;
    private ColorManager colorManager;
    private TestFileEditor editor;
    
    public TestFileConfiguration(final ColorManager colorManager, final TestFileEditor editor) {
        super();
        this.colorManager = colorManager;
        this.editor = editor;
    }
    
    public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
        return new String[] { "__dftl_partition_content_type", "__test_file_comment", "__test_name", "__lang_def", "__expected", "__class_name", "__file_name", "__selection" };
    }
    
    public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
        if (this.doubleClickStrategy == null) {
            this.doubleClickStrategy = new TestFileDoubleClickStrategy();
        }
        return (ITextDoubleClickStrategy)this.doubleClickStrategy;
    }
    
    public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
        final PresentationReconciler reconciler = new PresentationReconciler();
        NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.CDT_TEST_COMMENT)));
        reconciler.setDamager((IPresentationDamager)ndr, "__test_file_comment");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__test_file_comment");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, "__test_name");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__test_name");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.LANG_DEF)));
        reconciler.setDamager((IPresentationDamager)ndr, "__lang_def");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__lang_def");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.EXPECTED)));
        reconciler.setDamager((IPresentationDamager)ndr, "__expected");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__expected");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.CLASS_NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, "__class_name");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__class_name");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.FILE_NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, "__file_name");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__file_name");
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.SELECTION)));
        reconciler.setDamager((IPresentationDamager)ndr, "__selection");
        reconciler.setRepairer((IPresentationRepairer)ndr, "__selection");
        return (IPresentationReconciler)reconciler;
    }
    
    public IReconciler getReconciler(final ISourceViewer sourceViewer) {
        final TestFileReconcilingStrategy strategy = new TestFileReconcilingStrategy();
        strategy.setEditor(this.editor);
        final MonoReconciler reconciler = new MonoReconciler((IReconcilingStrategy)strategy, false);
        return (IReconciler)reconciler;
    }
}
