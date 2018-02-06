package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import name.graf.emanuel.testfileeditor.model.TestFile;


public class DocumentProvider extends FileDocumentProvider {

   @Override
   public void changed(final Object element) {
      super.changed(element);
   }

   @Override
   protected IDocument createDocument(final Object element) throws CoreException {
      final IDocument document = super.createDocument(element);
      if (document != null) {
         final IDocumentPartitioner partitioner = new FastPartitioner(new PartitionScanner(), TestFile.PARTITION_TYPES);
         partitioner.connect(document);
         document.setDocumentPartitioner(partitioner);
      }
      return document;
   }
}
