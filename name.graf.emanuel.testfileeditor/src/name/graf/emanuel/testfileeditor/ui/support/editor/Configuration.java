package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import name.graf.emanuel.testfileeditor.model.TestFile;
import name.graf.emanuel.testfileeditor.ui.Editor;


public class Configuration extends SourceViewerConfiguration {

   private DoubleClickStrategy doubleClickStrategy;
   private ColorManager        colorManager;
   private Editor              editor;

   public Configuration(final ColorManager colorManager, final Editor editor) {
      super();
      this.colorManager = colorManager;
      this.editor = editor;
   }

   public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
      return TestFile.PARTITION_TYPES;
   }

   public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
      if (this.doubleClickStrategy == null) {
         this.doubleClickStrategy = new DoubleClickStrategy();
      }
      return (ITextDoubleClickStrategy) this.doubleClickStrategy;
   }

   public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
      final PresentationReconciler reconciler = new PresentationReconciler();
      DamagerRepairer ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.CDT_TEST_COMMENT)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_COMMENT);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_COMMENT);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.NAME)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_NAME);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_NAME);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.LANG_DEF)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_LANGUAGE);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_LANGUAGE);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.EXPECTED)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_EXPECTED);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_EXPECTED);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.CLASS_NAME)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_CLASS);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_CLASS);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.FILE_NAME)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_FILE);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_FILE);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.SELECTION)));
      reconciler.setDamager((IPresentationDamager) ndr, TestFile.PARTITION_TEST_SELECTION);
      reconciler.setRepairer((IPresentationRepairer) ndr, TestFile.PARTITION_TEST_SELECTION);
      return (IPresentationReconciler) reconciler;
   }

   public IReconciler getReconciler(final ISourceViewer sourceViewer) {
      final ReconcilingStrategy strategy = new ReconcilingStrategy();
      strategy.setEditor(this.editor);
      final MonoReconciler reconciler = new MonoReconciler((IReconcilingStrategy) strategy, false);
      return (IReconciler) reconciler;
   }

   @Override
   public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
      return new DefaultAnnotationHover();
   }
}
