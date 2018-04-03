package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;

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

   @Override
   public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
      return TestFile.PARTITION_TYPES;
   }

   @Override
   public ITextDoubleClickStrategy getDoubleClickStrategy(final ISourceViewer sourceViewer, final String contentType) {
      if (this.doubleClickStrategy == null) {
         this.doubleClickStrategy = new DoubleClickStrategy();
      }
      return this.doubleClickStrategy;
   }

   @Override
   public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
      final PresentationReconciler reconciler = new PresentationReconciler();
      DamagerRepairer ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.CDT_TEST_COMMENT), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_COMMENT);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_COMMENT);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.TEST_NAME), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_NAME);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_NAME);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.LANG_DEF), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_LANGUAGE);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_LANGUAGE);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.EXPECTED), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_EXPECTED);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_EXPECTED);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.CLASS_NAME), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_CLASS);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_CLASS);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.FILE_NAME), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_FILE);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_FILE);
      ndr = new DamagerRepairer(new TextAttribute(this.colorManager.getColor(Colors.SELECTION), null, SWT.BOLD));
      reconciler.setDamager(ndr, TestFile.PARTITION_TEST_SELECTION);
      reconciler.setRepairer(ndr, TestFile.PARTITION_TEST_SELECTION);
      return reconciler;
   }

   @Override
   public IReconciler getReconciler(final ISourceViewer sourceViewer) {
      final ReconcilingStrategy strategy = new ReconcilingStrategy();
      strategy.setEditor(this.editor);
      final MonoReconciler reconciler = new MonoReconciler(strategy, false);
      return reconciler;
   }

   @Override
   public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
      return new DefaultAnnotationHover();
   }
}
