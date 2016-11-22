package name.graf.emanuel.testfileeditor.model;

import static name.graf.emanuel.testfileeditor.model.Tokens.*;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerUtilities;

import name.graf.emanuel.testfileeditor.Activator;
import name.graf.emanuel.testfileeditor.model.node.Class;
import name.graf.emanuel.testfileeditor.model.node.Expected;
import name.graf.emanuel.testfileeditor.model.node.File;
import name.graf.emanuel.testfileeditor.model.node.Language;
import name.graf.emanuel.testfileeditor.model.node.Selection;
import name.graf.emanuel.testfileeditor.model.node.Test;

public class TestFile extends Observable {
    private static final String MARKER_ID_DUPLICATE_TEST = "name.graf.emanuel.testfileeditor.markers.DuplicateTestMarker";
    
    private final String fName;
    private final Set<Test> fTests;
    private final Set<Problem> fProblems;
    private final IDocumentProvider fProvider;
    private FileEditorInput fInput;

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

    public TestFile(FileEditorInput input, IDocumentProvider provider) {
        fInput = input;
        fProvider = provider;
        fName = input.getName();
        fTests = new HashSet<>();
        fProblems = new HashSet<>();
        setupPositionCategories();
    }

    private void setupPositionCategories() {
        IDocument document = fProvider.getDocument(fInput);
        for (String partition : PARTITION_TYPES) {
            document.addPositionCategory(partition);
        }
    }
    
    public TestFile(FileEditorInput input, IDocumentProvider provider, Observer observer) {
        this(input, provider);
        addObserver(observer);
    }

    @Override
    public String toString() {
        return this.fName;
    }

    public Test[] getTests() {
        return this.fTests.toArray(new Test[0]);
    }

    public Set<Problem> getProblems() {
        return fProblems;
    }
    
    @Override
    public int hashCode() {
        return this.fName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
    
    public void parse() {
        try {
            doParse();
            notifyObservers();
        } catch (BadLocationException | BadPositionCategoryException | CoreException e) {
            Activator.logError(e, 0);
        }
    }

    private void doParse() throws BadLocationException, BadPositionCategoryException, CoreException {
        final IDocument document = fProvider.getDocument(fInput);
        final int NOF_LINES = document.getNumberOfLines();

        fTests.clear();
        fProblems.clear();
        cleanMarkers();
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
                if (fTests.contains(currentTest)) {
                    Problem duplicateTest = new DuplicateTest(currentTest.toString(), currentLine + 1, tagPosition);
                    reportProblem(duplicateTest);
                    fProblems.add(new DuplicateTest(currentTest.toString(), currentLine + 1, tagPosition));
                } else {
                    fTests.add(currentTest);
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

    private void reportProblem(Problem problem) throws CoreException {
        IFile file = fInput.getFile();
        IMarker marker = file.createMarker(MARKER_ID_DUPLICATE_TEST);
        MarkerUtilities.setCharStart(marker, problem.getPosition().offset);
        MarkerUtilities.setCharEnd(marker, problem.getPosition().offset + problem.getPosition().length);
        MarkerUtilities.setLineNumber(marker, problem.getLineNumber());
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
        marker.setAttribute(IMarker.MESSAGE, problem.getDescription());
    }

    private void cleanMarkers() {
        try {
            IMarker[] currentMarkers = fInput.getFile().findMarkers(MARKER_ID_DUPLICATE_TEST, false, IFile.DEPTH_INFINITE);
            for (IMarker marker : currentMarkers) {
                int offset = MarkerUtilities.getCharStart(marker);
                int length = MarkerUtilities.getCharEnd(marker) - offset;
                Position position = new Position(offset, length);

                Problem found = null;
                for (Problem problem : fProblems) {
                    if (problem.getPosition().equals(position)) {
                        found = problem;
                        break;
                    }
                }

                if (found == null) {
                    marker.delete();
                } else {
                    fProblems.remove(found);
                }
            }
        } catch (CoreException e) {
            Activator.logError(e, 0);
        }
    }

    private enum ParseState {
        INIT, TEST, FILE, SELECTION
    }

    public void setInput(FileEditorInput input) {
        fInput = input;
        setupPositionCategories();
    }
}
