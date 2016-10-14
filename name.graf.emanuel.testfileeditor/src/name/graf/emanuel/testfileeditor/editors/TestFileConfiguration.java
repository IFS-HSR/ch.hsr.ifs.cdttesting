package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.source.*;

import name.graf.emanuel.testfileeditor.ui.TestFile;

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
        return TestFile.PARTITION_TYPES;
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
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_COMMENT);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_COMMENT);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_NAME);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_NAME);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.LANG_DEF)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_LANGUAGE);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_LANGUAGE);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.EXPECTED)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_EXPECTED);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_EXPECTED);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.CLASS_NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_CLASS);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_CLASS);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.FILE_NAME)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_FILE);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_FILE);
        ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(this.colorManager.getColor(ICdtTestFileColorConstants.SELECTION)));
        reconciler.setDamager((IPresentationDamager)ndr, TestFile.PARTITION_TEST_SELECTION);
        reconciler.setRepairer((IPresentationRepairer)ndr, TestFile.PARTITION_TEST_SELECTION);
        return (IPresentationReconciler)reconciler;
    }
    
    public IReconciler getReconciler(final ISourceViewer sourceViewer) {
        final TestFileReconcilingStrategy strategy = new TestFileReconcilingStrategy();
        strategy.setEditor(this.editor);
        final MonoReconciler reconciler = new MonoReconciler((IReconcilingStrategy)strategy, false);
        return (IReconciler)reconciler;
    }
}
