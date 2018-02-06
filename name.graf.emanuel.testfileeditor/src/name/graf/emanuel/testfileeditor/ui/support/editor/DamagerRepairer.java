package name.graf.emanuel.testfileeditor.ui.support.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;


public class DamagerRepairer implements IPresentationDamager, IPresentationRepairer {

   protected IDocument     fDocument;
   protected TextAttribute fDefaultTextAttribute;

   public DamagerRepairer(final TextAttribute defaultTextAttribute) {
      super();
      // Assert.isNotNull((Object)defaultTextAttribute);
      this.fDefaultTextAttribute = defaultTextAttribute;
   }

   @Override
   public void setDocument(final IDocument document) {
      this.fDocument = document;
   }

   protected int endOfLineOf(final int offset) throws BadLocationException {
      IRegion info = this.fDocument.getLineInformationOfOffset(offset);
      if (offset <= info.getOffset() + info.getLength()) { return info.getOffset() + info.getLength(); }
      final int line = this.fDocument.getLineOfOffset(offset);
      try {
         info = this.fDocument.getLineInformation(line + 1);
         return info.getOffset() + info.getLength();
      } catch (BadLocationException ex) {
         return this.fDocument.getLength();
      }
   }

   @Override
   public IRegion getDamageRegion(final ITypedRegion partition, final DocumentEvent event, final boolean documentPartitioningChanged) {
      if (!documentPartitioningChanged) {
         try {
            final IRegion info = this.fDocument.getLineInformationOfOffset(event.getOffset());
            final int start = Math.max(partition.getOffset(), info.getOffset());
            int end = event.getOffset() + ((event.getText() == null) ? event.getLength() : event.getText().length());
            if (info.getOffset() <= end && end <= info.getOffset() + info.getLength()) {
               end = info.getOffset() + info.getLength();
            } else {
               end = this.endOfLineOf(end);
            }
            end = Math.min(partition.getOffset() + partition.getLength(), end);
            return new Region(start, end - start);
         } catch (BadLocationException ex) {}
      }
      return partition;
   }

   @Override
   public void createPresentation(final TextPresentation presentation, final ITypedRegion region) {
      this.addRange(presentation, region.getOffset(), region.getLength(), this.fDefaultTextAttribute);
   }

   protected void addRange(final TextPresentation presentation, final int offset, final int length, final TextAttribute attr) {
      if (attr != null) {
         presentation.addStyleRange(new StyleRange(offset, length, attr.getForeground(), attr.getBackground(), attr.getStyle()));
      }
   }
}
