package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.ui.editors.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;
import org.eclipse.core.runtime.*;

public class TestFileDocumentProvider extends FileDocumentProvider
{
    protected IDocument createDocument(final Object element) throws CoreException {
        final IDocument document = super.createDocument(element);
        if (document != null) {
            final IDocumentPartitioner partitioner = (IDocumentPartitioner)new FastPartitioner((IPartitionTokenScanner)new TestFilePartitionScanner(), new String[] { "__test_name", "__test_file_comment", "__lang_def", "__expected", "__class_name", "__file_name", "__selection" });
            partitioner.connect(document);
            document.setDocumentPartitioner(partitioner);
        }
        return document;
    }
    
    public void changed(final Object element) {
        super.changed(element);
    }
}
