package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.core.filebuffers.*;
import name.graf.emanuel.testfileeditor.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class TestFileDocumentSetupParticipant implements IDocumentSetupParticipant
{
    public void setup(final IDocument document) {
        if (document instanceof IDocumentExtension3) {
            final IDocumentExtension3 extension3 = (IDocumentExtension3)document;
            final IDocumentPartitioner partitioner = (IDocumentPartitioner)new FastPartitioner((IPartitionTokenScanner)Activator.getDefault().getTestFilePartitionScanner(), TestFilePartitionScanner.TEST_FILE_PARTITION_TYPES);
            extension3.setDocumentPartitioner("__test_file_partitioning", partitioner);
            partitioner.connect(document);
        }
    }
}
