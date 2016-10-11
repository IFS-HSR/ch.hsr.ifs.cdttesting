package name.graf.emanuel.testfileeditor.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import name.graf.emanuel.testfileeditor.Activator;

public class TestFile extends Observable {
    private final String name;
    private final ArrayList<Test> tests;
    private IDocument document;

    private final IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);

    //@formatter:off
    private final String TAG_TEST = preferences.get(
            PreferenceConstants.P_TEST_NAME_START,
            PreferenceConstants.D_TEST_NAME_START
            );

    private final String TAG_LANGUAGE = preferences.get(
            PreferenceConstants.P_LANG_START,
            PreferenceConstants.D_LANG_START
            );

    private final String TAG_EXPECTED = preferences.get(
            PreferenceConstants.P_EXPECTED_START,
            PreferenceConstants.D_EXPECTED_START
            );

    private final String TAG_FILE = preferences.get(
            PreferenceConstants.P_FILE_NAME,
            PreferenceConstants.D_FILE_NAME
            );

    private final String TAG_CLASS = preferences.get(
            PreferenceConstants.P_CLASS_NAME,
            PreferenceConstants.D_CLASS_NAME
            );

    private final String TAG_SELECTION_START = preferences.get(
            PreferenceConstants.P_SELECTION_START,
            PreferenceConstants.D_SELECTION_START
            );

    private final String TAG_SELECTION_END = preferences.get(
            PreferenceConstants.P_SELECTION_END,
            PreferenceConstants.D_SELECTION_END
            );
    //@formatter:on

    public static final String POSITION_TEST = "__cdttest_test";
    public static final String POSITION_LANGUAGE = "__cdttest_language";
    public static final String POSITION_SELECTION = "__cdttest_selection";
    public static final String POSITION_EXPECTED = "__cdttest_expected";
    public static final String POSITION_FILE = "__cdttest_file";
    public static final String POSITION_CLASS = "__cdttest_class";

    private static final List<String> POSITION_CATEGORIES = new ArrayList<>(6);

    static {
        POSITION_CATEGORIES.add(POSITION_TEST);
        POSITION_CATEGORIES.add(POSITION_LANGUAGE);
        POSITION_CATEGORIES.add(POSITION_SELECTION);
        POSITION_CATEGORIES.add(POSITION_EXPECTED);
        POSITION_CATEGORIES.add(POSITION_FILE);
        POSITION_CATEGORIES.add(POSITION_CLASS);
    }

    public TestFile(final String name) {
        this.name = name;
        tests = new ArrayList<Test>();
        document = null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Test[] getTests() {
        return this.tests.toArray(new Test[0]);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    public void setDocument(final IDocument document) {
        try {
            if (this.document != null) {
                for (String category : POSITION_CATEGORIES) {
                    this.document.removePositionCategory(category);
                }
            }

            this.document = document;
            if (this.document != null) {
                for (String category : POSITION_CATEGORIES) {
                    this.document.addPositionCategory(category);
                }
                parse();
                notifyObservers();
            }
        } catch (BadLocationException | BadPositionCategoryException e) {
        }
    }

    public IDocument getDocument() {
        return document;
    }

    private void parse() throws BadLocationException, BadPositionCategoryException {
        final int NOF_LINES = document.getNumberOfLines();

        tests.clear();
        Test currentTest = null;
        FileDefNode currentFile = null;
        ParseState currentState = ParseState.INIT;
        int currentSelectionStart = 0;

        for (int currentLine = 0; currentLine < NOF_LINES; ++currentLine) {
            final int lineOffset = document.getLineOffset(currentLine);
            final int lineLength = document.getLineLength(currentLine);
            final String lineContent = document.get(lineOffset, lineLength);

            if (lineContent.startsWith(TAG_TEST)) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(POSITION_TEST, tagPosition);
                currentTest = new Test(lineContent.substring(TAG_TEST.length()).trim(), tagPosition, this);
                tests.add(currentTest);
                currentState = ParseState.TEST;
            } else if (lineContent.startsWith(TAG_LANGUAGE) && currentTest != null) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(POSITION_LANGUAGE, tagPosition);
                currentTest.setLang(
                        new LanguageDef(lineContent.substring(TAG_LANGUAGE.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.startsWith(TAG_EXPECTED)) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(POSITION_EXPECTED, tagPosition);
                    currentFile.setSelection(new SelectionNode(tagPosition, currentFile));
                    currentState = ParseState.FILE;
                }

                switch (currentState) {
                case FILE:
                    if (currentTest != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(POSITION_EXPECTED, tagPosition);
                        currentTest.setExpected(new ExpectedNode(currentTest,
                                lineContent.substring(TAG_EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                case SELECTION:
                    if (currentFile != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(POSITION_SELECTION, tagPosition);
                        currentFile.setExpected(new ExpectedNode(currentTest,
                                lineContent.substring(TAG_EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                default:
                }
            } else if (lineContent.startsWith(TAG_FILE) && currentTest != null) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(POSITION_SELECTION, tagPosition);
                    currentFile.setSelection(new SelectionNode(tagPosition, currentFile));
                }
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(POSITION_FILE, tagPosition);
                currentFile = new FileDefNode(lineContent.substring(TAG_FILE.length()).trim(), tagPosition,
                        currentTest);
                currentTest.addFile(currentFile);
                currentState = ParseState.FILE;
            } else if (lineContent.startsWith(TAG_CLASS) && currentState == ParseState.TEST) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(POSITION_CLASS, tagPosition);
                currentTest.setClassname(
                        new ClassNameNode(lineContent.substring(TAG_CLASS.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.contains(TAG_SELECTION_START) && currentState == ParseState.FILE) {
                currentState = ParseState.SELECTION;
                currentSelectionStart = lineOffset + lineContent.indexOf(TAG_SELECTION_START);
            }
            if (lineContent.contains(TAG_SELECTION_END) && currentState == ParseState.SELECTION) {
                final Position tagPosition = new Position(currentSelectionStart,
                        lineOffset + lineContent.indexOf(TAG_SELECTION_END) - currentSelectionStart);
                document.addPosition(POSITION_SELECTION, tagPosition);
                currentFile.setSelection(new SelectionNode(tagPosition, currentFile));
                currentState = ParseState.FILE;
            }
        }

        setChanged();
    }

    private enum ParseState {
        INIT, TEST, FILE, SELECTION
    }
}
