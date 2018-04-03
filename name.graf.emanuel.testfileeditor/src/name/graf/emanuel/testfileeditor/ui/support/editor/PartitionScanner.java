package name.graf.emanuel.testfileeditor.ui.support.editor;

import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_CLASS;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_COMMENT;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_EXPECTED;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_FILE;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_LANGUAGE;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_NAME;
import static name.graf.emanuel.testfileeditor.model.TestFile.PARTITION_TEST_SELECTION;
import static name.graf.emanuel.testfileeditor.model.Tokens.CLASS;
import static name.graf.emanuel.testfileeditor.model.Tokens.COMMENT_CLOSE;
import static name.graf.emanuel.testfileeditor.model.Tokens.COMMENT_OPEN;
import static name.graf.emanuel.testfileeditor.model.Tokens.EXPECTED;
import static name.graf.emanuel.testfileeditor.model.Tokens.FILE;
import static name.graf.emanuel.testfileeditor.model.Tokens.LANGUAGE;
import static name.graf.emanuel.testfileeditor.model.Tokens.SELECTION_CLOSE;
import static name.graf.emanuel.testfileeditor.model.Tokens.SELECTION_OPEN;
import static name.graf.emanuel.testfileeditor.model.Tokens.TEST;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;


public class PartitionScanner extends RuleBasedPartitionScanner {

   private final IToken clazz     = new Token(PARTITION_TEST_CLASS);
   private final IToken comment   = new Token(PARTITION_TEST_COMMENT);
   private final IToken expected  = new Token(PARTITION_TEST_EXPECTED);
   private final IToken file      = new Token(PARTITION_TEST_FILE);
   private final IToken language  = new Token(PARTITION_TEST_LANGUAGE);
   private final IToken name      = new Token(PARTITION_TEST_NAME);
   private final IToken selection = new Token(PARTITION_TEST_SELECTION);

   public PartitionScanner() {
      super();

      //@formatter:off
        final IPredicateRule[] rules = new IPredicateRule[] {
                new EndOfLineRule(CLASS, clazz),
                new EndOfLineRule(EXPECTED, expected),
                new EndOfLineRule(FILE, file),
                new EndOfLineRule(LANGUAGE, language),
                new EndOfLineRule(TEST, name),
                new MultiLineRule(SELECTION_OPEN, SELECTION_CLOSE, selection),
                new MultiLineRule(COMMENT_OPEN, COMMENT_CLOSE, comment),
        };
        //@formattor:on

        this.setPredicateRules(rules);
    }
}
