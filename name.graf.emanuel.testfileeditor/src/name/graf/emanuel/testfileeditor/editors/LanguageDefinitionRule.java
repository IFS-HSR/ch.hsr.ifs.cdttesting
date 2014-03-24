package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.jface.text.rules.*;

public class LanguageDefinitionRule extends EndOfLineRule
{
    public LanguageDefinitionRule(final String start, final IToken token) {
        super(start, token);
    }
}
