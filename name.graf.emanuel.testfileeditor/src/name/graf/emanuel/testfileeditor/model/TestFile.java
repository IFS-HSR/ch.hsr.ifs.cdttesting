package name.graf.emanuel.testfileeditor.model;

import static name.graf.emanuel.testfileeditor.model.Tokens.*;

import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.model.node.Class;
import name.graf.emanuel.testfileeditor.model.node.Expected;
import name.graf.emanuel.testfileeditor.model.node.File;
import name.graf.emanuel.testfileeditor.model.node.Language;
import name.graf.emanuel.testfileeditor.model.node.Selection;
import name.graf.emanuel.testfileeditor.model.node.Test;

public class TestFile extends Observable {
    private final String name;
    private final Set<Test> tests;
    private final Set<Problem> problems;
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
        tests = new HashSet<>();
        problems = new HashSet<>();
        document = null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Test[] getTests() {
        return this.tests.toArray(new Test[0]);
    }

    public Set<Problem> getProblems() {
        return problems;
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
                if(this.document.equals(document)){
                    return;
                }
                
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
    
    public void reparse() {
        try {
            parse();
            notifyObservers();
        } catch (BadLocationException | BadPositionCategoryException e) {
            Activator.logError(e, 0);
        }
    }

    public IDocument getDocument() {
        return document;
    }

    private void parse() throws BadLocationException, BadPositionCategoryException {
        final int NOF_LINES = document.getNumberOfLines();

        tests.clear();
        problems.clear();
        Test currentTest = null;
        File currentFile = null;
        ParseState currentState = ParseState.INIT;
        int currentSelectionStart = 0;

        for (int currentLine = 0; currentLine < NOF_LINES; ++currentLine) {
            final int lineOffset = document.getLineOffset(currentLine);
            final int lineLength = document.getLineLength(currentLine);
            final String lineContent = document.get(lineOffset, lineLength);

            if (lineContent.startsWith(TEST)) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_NAME, tagPosition);
                currentTest = new Test(lineContent.substring(TEST.length()).trim(), tagPosition, this);
                if (tests.contains(currentTest)) {
                    problems.add(new DuplicateTest(currentTest.toString(), currentLine + 1, tagPosition));
                } else {
                    tests.add(currentTest);
                }
                currentState = ParseState.TEST;
            } else if (lineContent.startsWith(LANGUAGE) && currentTest != null) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_LANGUAGE, tagPosition);
                currentTest.setLang(
                        new Language(lineContent.substring(LANGUAGE.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.startsWith(EXPECTED)) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(PARTITION_TEST_EXPECTED, tagPosition);
                    currentFile.setSelection(new Selection(tagPosition, currentFile));
                    currentState = ParseState.FILE;
                }

                switch (currentState) {
                case FILE:
                    if (currentTest != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(PARTITION_TEST_EXPECTED, tagPosition);
                        currentTest.setExpected(new Expected(currentTest,
                                lineContent.substring(EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                case SELECTION:
                    if (currentFile != null) {
                        final Position tagPosition = new Position(lineOffset, lineLength);
                        document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
                        currentFile.setExpected(new Expected(currentTest,
                                lineContent.substring(EXPECTED.length()).trim(), tagPosition));
                    }
                    break;
                default:
                }
            } else if (lineContent.startsWith(FILE) && currentTest != null) {
                if (currentState == ParseState.SELECTION) {
                    final Position tagPosition = new Position(currentSelectionStart,
                            lineOffset - currentSelectionStart);
                    document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
                    currentFile.setSelection(new Selection(tagPosition, currentFile));
                }
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_FILE, tagPosition);
                currentFile = new File(lineContent.substring(FILE.length()).trim(), tagPosition, currentTest);
                currentTest.addFile(currentFile);
                currentState = ParseState.FILE;
            } else if (lineContent.startsWith(CLASS) && currentState == ParseState.TEST) {
                final Position tagPosition = new Position(lineOffset, lineLength);
                document.addPosition(PARTITION_TEST_CLASS, tagPosition);
                currentTest.setClassname(
                        new Class(lineContent.substring(CLASS.length()).trim(), tagPosition, currentTest));
            } else if (lineContent.contains(SELECTION_OPEN) && currentState == ParseState.FILE) {
                currentState = ParseState.SELECTION;
                currentSelectionStart = lineOffset + lineContent.indexOf(SELECTION_CLOSE);
            }
            if (lineContent.contains(SELECTION_CLOSE) && currentState == ParseState.SELECTION) {
                final Position tagPosition = new Position(currentSelectionStart,
                        lineOffset + lineContent.indexOf(SELECTION_CLOSE) - currentSelectionStart);
                document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
                currentFile.setSelection(new Selection(tagPosition, currentFile));
                currentState = ParseState.FILE;
            }
        }

        setChanged();
    }

    private enum ParseState {
        INIT, TEST, FILE, SELECTION
    }
}
