package name.graf.emanuel.testfileeditor.ui;

import static name.graf.emanuel.testfileeditor.TestfileLanguage.*;

import java.util.ArrayList;
import java.util.Observable;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class TestFile extends Observable {
    private final String name;
    private final ArrayList<Test> tests;
    private IDocument document;

    public static final String PARTITION_TEST_CLASS = "__rts_class";
    public static final String PARTITION_TEST_COMMENT = "__rts_comment";
    public static final String PARTITION_TEST_EXPECTED = "__rts_expected";
    public static final String PARTITION_TEST_FILE = "__rts_file";
    public static final String PARTITION_TEST_LANGUAGE = "__rts_language";
    public static final String PARTITION_TEST_NAME = "__rts_name";
    public static final String PARTITION_TEST_SELECTION = "__rts_selection";

    //@formatter:off
    public static final String[] PARTITION_TYPES = new String[] {
            PARTITION_TEST_CLASS,
            PARTITION_TEST_COMMENT,
            PARTITION_TEST_EXPECTED,
            PARTITION_TEST_FILE,
            PARTITION_TEST_LANGUAGE,
            PARTITION_TEST_NAME,
            PARTITION_TEST_SELECTION
    };
    //@formatter:on

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
                for (String category : PARTITION_TYPES) {
                    this.document.removePositionCategory(category);
                }
            }

            this.document = document;
            if (this.document != null) {
                for (String category : PARTITION_TYPES) {
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

            if (lineContent.startsWith(TOKEN_TEST_NAME)) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_NAME, tagPosition);
                currentTest = new Test(lineContent.substring(TOKEN_TEST_NAME.length()).trim(), tagPosition, this);
                tests.add(currentTest);
                currentState = ParseState.TEST;
            } else if (lineContent.startsWith(TOKEN_TEST_LANGUAGE) && currentTest != null) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_LANGUAGE, tagPosition);
                currentTest.setLang(
                        new LanguageDef(lineContent.substring(TOKEN_TEST_LANGUAGE.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.startsWith(TOKEN_TEST_EXPECTED)) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(PARTITION_TEST_EXPECTED, tagPosition);
                    currentFile.setSelection(new SelectionNode(tagPosition, currentFile));
                    currentState = ParseState.FILE;
                }

                switch (currentState) {
                case FILE:
                    if (currentTest != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(PARTITION_TEST_EXPECTED, tagPosition);
                        currentTest.setExpected(new ExpectedNode(currentTest,
                                lineContent.substring(TOKEN_TEST_EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                case SELECTION:
                    if (currentFile != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
                        currentFile.setExpected(new ExpectedNode(currentTest,
                                lineContent.substring(TOKEN_TEST_EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                default:
                }
            } else if (lineContent.startsWith(TOKEN_TEST_FILE) && currentTest != null) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
                    currentFile.setSelection(new SelectionNode(tagPosition, currentFile));
                }
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_FILE, tagPosition);
                currentFile = new FileDefNode(lineContent.substring(TOKEN_TEST_FILE.length()).trim(), tagPosition,
                        currentTest);
                currentTest.addFile(currentFile);
                currentState = ParseState.FILE;
            } else if (lineContent.startsWith(TOKEN_TEST_CLASS) && currentState == ParseState.TEST) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_CLASS, tagPosition);
                currentTest.setClassname(
                        new ClassNameNode(lineContent.substring(TOKEN_TEST_CLASS.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.contains(TOKEN_TEST_SELECTION_OPEN) && currentState == ParseState.FILE) {
                currentState = ParseState.SELECTION;
                currentSelectionStart = lineOffset + lineContent.indexOf(TOKEN_TEST_SELECTION_CLOSE);
            }
            if (lineContent.contains(TOKEN_TEST_SELECTION_CLOSE) && currentState == ParseState.SELECTION) {
                final Position tagPosition = new Position(currentSelectionStart,
                        lineOffset + lineContent.indexOf(TOKEN_TEST_SELECTION_CLOSE) - currentSelectionStart);
                document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
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
