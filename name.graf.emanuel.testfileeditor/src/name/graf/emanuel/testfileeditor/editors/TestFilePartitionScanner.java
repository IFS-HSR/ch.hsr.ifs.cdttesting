package name.graf.emanuel.testfileeditor.editors;

import name.graf.emanuel.testfileeditor.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.core.runtime.*;

public class TestFilePartitionScanner extends RuleBasedPartitionScanner
{
    public static final String CDT_TEST_FIEL_COMMENT = "__test_file_comment";
    public static final String TEST_NAME = "__test_name";
    public static final String LANG_DEF = "__lang_def";
    public static final String EXPECTED = "__expected";
    public static final String CLASS_NAME = "__class_name";
    public static final String SELECTION = "__selection";
    public static final String FILE_NAME = "__file_name";
    public static final String[] TEST_FILE_PARTITION_TYPES;
    
    static {
        TEST_FILE_PARTITION_TYPES = new String[] { "__test_file_comment", "__test_name", "__lang_def", "__expected" };
    }
    
    public TestFilePartitionScanner() {
        super();
        final IToken cdtTestFileComment = (IToken)new Token((Object)"__test_file_comment");
        final IToken testName = (IToken)new Token((Object)"__test_name");
        final IToken langDef = (IToken)new Token((Object)"__lang_def");
        final IToken expected = (IToken)new Token((Object)"__expected");
        final IToken selection = (IToken)new Token((Object)"__selection");
        final IToken className = (IToken)new Token((Object)"__class_name");
        final IToken fileName = (IToken)new Token((Object)"__file_name");
        final IPredicateRule[] rules = new IPredicateRule[7];
        final Preferences pref = Activator.getDefault().getPluginPreferences();
        final String nameStartTag = pref.getString("nameStart");
        rules[0] = (IPredicateRule)new MultiLineRule(pref.getString("comStart"), pref.getString("comEnd"), cdtTestFileComment);
        rules[1] = (IPredicateRule)new EndOfLineRule(nameStartTag, testName);
        rules[2] = (IPredicateRule)new EndOfLineRule(pref.getString("langStart"), langDef);
        rules[3] = (IPredicateRule)new EndOfLineRule(pref.getString("expecStart"), expected);
        rules[4] = (IPredicateRule)new MultiLineRule(pref.getString("selectionStart"), pref.getString("selectionEnd"), selection);
        rules[5] = (IPredicateRule)new EndOfLineRule(pref.getString("className"), className);
        rules[6] = (IPredicateRule)new EndOfLineRule(pref.getString("fileName"), fileName);
        this.setPredicateRules(rules);
    }
}
