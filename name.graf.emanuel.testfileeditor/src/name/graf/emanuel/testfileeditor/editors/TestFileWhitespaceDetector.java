package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.rules.*;

public class TestFileWhitespaceDetector implements IWhitespaceDetector
{
    public boolean isWhitespace(final char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
