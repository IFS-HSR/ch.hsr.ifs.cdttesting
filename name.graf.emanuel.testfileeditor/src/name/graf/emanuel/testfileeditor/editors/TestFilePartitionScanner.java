package name.graf.emanuel.testfileeditor.editors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.ui.PreferenceConstants;

public class TestFilePartitionScanner extends RuleBasedPartitionScanner {
	public static final String COMMENT = "__test_file_comment";
	public static final String TEST_NAME = "__test_name";
	public static final String LANG_DEF = "__lang_def";
	public static final String EXPECTED = "__expected";
	public static final String CLASS_NAME = "__class_name";
	public static final String SELECTION = "__selection";
	public static final String FILE_NAME = "__file_name";
	public static final String[] TEST_FILE_PARTITION_TYPES;

	static {
		TEST_FILE_PARTITION_TYPES = new String[] { COMMENT, TEST_NAME, LANG_DEF, EXPECTED };
	}

	public TestFilePartitionScanner() {
		super();
		final IToken comment = new Token(COMMENT);
		final IToken testName = new Token(TEST_NAME);
		final IToken langDef = new Token(LANG_DEF);
		final IToken expected = new Token(EXPECTED);
		final IToken selection = new Token(SELECTION);
		final IToken className = new Token(CLASS_NAME);
		final IToken fileName = new Token(FILE_NAME);

		final IPredicateRule[] rules = new IPredicateRule[7];

		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		rules[0] = new MultiLineRule(
				preferences.get(PreferenceConstants.P_COMMENT_START, PreferenceConstants.D_COMMENT_START),
				preferences.get(PreferenceConstants.P_COMMENT_END, PreferenceConstants.D_COMMENT_END),
				comment);
		rules[1] = new EndOfLineRule(
				preferences.get(PreferenceConstants.P_TEST_NAME_START, PreferenceConstants.D_TEST_NAME_START),
				testName);
		rules[2] = new EndOfLineRule(
				preferences.get(PreferenceConstants.P_LANG_START, PreferenceConstants.D_LANG_START), langDef);
		rules[3] = new EndOfLineRule(
				preferences.get(PreferenceConstants.P_EXPECTED_START, PreferenceConstants.D_EXPECTED_START), expected);
		rules[4] = new MultiLineRule(
				preferences.get(PreferenceConstants.P_SELECTION_START, PreferenceConstants.D_SELECTION_START),
				preferences.get(PreferenceConstants.P_SELECTION_END, PreferenceConstants.D_SELECTION_END), selection);
		rules[5] = new EndOfLineRule(
				preferences.get(PreferenceConstants.P_CLASS_NAME, PreferenceConstants.D_CLASS_NAME), className);
		rules[6] = new EndOfLineRule(preferences.get(PreferenceConstants.P_FILE_NAME, PreferenceConstants.D_FILE_NAME),
				fileName);
		this.setPredicateRules(rules);
	}
}
