package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.rules.*;

public class ExpectedRule extends EndOfLineRule
{
    public ExpectedRule(final String start, final IToken token) {
        super(start, token);
    }
}
