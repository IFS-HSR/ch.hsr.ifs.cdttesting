package name.graf.emanuel.testfileeditor.editors;

import static name.graf.emanuel.testfileeditor.TestfileLanguage.*;
import static name.graf.emanuel.testfileeditor.ui.TestFile.*;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class TestFilePartitionScanner extends RuleBasedPartitionScanner {
    
    private final IToken clazz = new Token(PARTITION_TEST_CLASS);
    private final IToken comment = new Token(PARTITION_TEST_COMMENT);
    private final IToken expected = new Token(PARTITION_TEST_EXPECTED);
    private final IToken file = new Token(PARTITION_TEST_FILE);
    private final IToken language = new Token(PARTITION_TEST_LANGUAGE);
    private final IToken name = new Token(PARTITION_TEST_NAME);
    private final IToken selection = new Token(PARTITION_TEST_SELECTION);

    public TestFilePartitionScanner() {
        super();

        //@formatter:off
        final IPredicateRule[] rules = new IPredicateRule[] {
                new EndOfLineRule(TOKEN_TEST_CLASS, clazz),
                new MultiLineRule(TOKEN_TEST_COMMENT_OPEN, TOKEN_TEST_COMMENT_CLOSE, comment),
                new EndOfLineRule(TOKEN_TEST_EXPECTED, expected),
                new EndOfLineRule(TOKEN_TEST_FILE, file),
                new EndOfLineRule(TOKEN_TEST_LANGUAGE, language),
                new EndOfLineRule(TOKEN_TEST_NAME, name),
                new MultiLineRule(TOKEN_TEST_SELECTION_OPEN, TOKEN_TEST_SELECTION_CLOSE, selection)
        };
        //@formattor:on

        this.setPredicateRules(rules);
    }
}
