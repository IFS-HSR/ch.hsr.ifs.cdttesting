package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.*;

public class TestFileDoubleClickStrategy implements ITextDoubleClickStrategy
{
    protected ITextViewer fText;
    
    public void doubleClicked(final ITextViewer part) {
        final int pos = part.getSelectedRange().x;
        if (pos < 0) {
            return;
        }
        this.fText = part;
        if (!this.selectComment(pos)) {
            this.selectWord(pos);
        }
    }
    
    protected boolean selectComment(final int caretPos) {
        final IDocument doc = this.fText.getDocument();
        try {
            int pos = caretPos;
            char c = ' ';
            while (pos >= 0) {
                c = doc.getChar(pos);
                if (c == '\\') {
                    pos -= 2;
                }
                else {
                    if (c == '\r') {
                        break;
                    }
                    if (c == '\"') {
                        break;
                    }
                    --pos;
                }
            }
            if (c != '\"') {
                return false;
            }
            final int startPos = pos;
            pos = caretPos;
            final int length = doc.getLength();
            c = ' ';
            while (pos < length) {
                c = doc.getChar(pos);
                if (c == '\r') {
                    break;
                }
                if (c == '\"') {
                    break;
                }
                ++pos;
            }
            if (c != '\"') {
                return false;
            }
            final int endPos = pos;
            final int offset = startPos + 1;
            final int len = endPos - offset;
            this.fText.setSelectedRange(offset, len);
            return true;
        }
        catch (BadLocationException ex) {
            return false;
        }
    }
    
    protected boolean selectWord(final int caretPos) {
        final IDocument doc = this.fText.getDocument();
        try {
            int pos;
            for (pos = caretPos; pos >= 0; --pos) {
                final char c = doc.getChar(pos);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
            }
            final int startPos = pos;
            pos = caretPos;
            for (int length = doc.getLength(); pos < length; ++pos) {
                final char c = doc.getChar(pos);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
            }
            final int endPos = pos;
            this.selectRange(startPos, endPos);
            return true;
        }
        catch (BadLocationException ex) {
            return false;
        }
    }
    
    private void selectRange(final int startPos, final int stopPos) {
        final int offset = startPos + 1;
        final int length = stopPos - offset;
        this.fText.setSelectedRange(offset, length);
    }
}
