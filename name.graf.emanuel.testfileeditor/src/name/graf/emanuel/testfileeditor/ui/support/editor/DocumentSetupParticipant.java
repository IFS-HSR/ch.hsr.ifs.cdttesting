package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.model.TestFile;


public class DocumentSetupParticipant implements IDocumentSetupParticipant {

   @Override
   public void setup(final IDocument document) {
      if (document instanceof IDocumentExtension3) {
         final IDocumentExtension3 extension3 = (IDocumentExtension3) document;
         final IDocumentPartitioner partitioner = new FastPartitioner(Activator.getDefault().getTestFilePartitionScanner(), TestFile.PARTITION_TYPES);
         extension3.setDocumentPartitioner("__rts_partitioning", partitioner);
         partitioner.connect(document);
      }
   }
}
