package name.graf.emanuel.testfileeditor.model;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public TestFile(final FileEditorInput input, final IDocumentProvider provider) {
		fInput = input;
		fProvider = provider;
		fName = input.getName();
		fTests = new HashSet<>();
		fProblems = new HashSet<>();
		setupPositionCategories();
	}

	/**
	 * @author tstauber
	 *
	 *         Appends the passed relativeLineNo to the markerLines line in the
	 *         .config file associated with the <code>Test</code> which contains
	 *         the absolute lineNo passed. If no <code>Test</code> or markerList
	 *         could be found the function will not change the file and returns
	 *         -1.
	 *
	 * @param int
	 *            lineNo Absolute line number in the file
	 * @param int
	 *            relativeLineNo Relative line number to the virtual file
	 * @return int The length difference from before to after.
	 */
	public int addLineNoToMarkerList(final int lineNo, final int relativeLineNo) {
		final IDocument document = fProvider.getDocument(fInput);
		int additionalLength = -1;

		Position precursor = new Position(0);
		Position successor = new Position(document.getLength() - 1);

		int precursorDist = document.getLength() - 1;
		int successorDist = document.getLength() - 1;

		try {
			final int offset = document.getLineOffset(lineNo);
			for (final Test test : fTests) {
				final int testOffset = test.getPosition().getOffset();
				if (testOffset < offset) {
					if (offset - testOffset < precursorDist) {
						precursorDist = offset - testOffset;
						precursor = test.getPosition();
					}
				} else {
					if (testOffset - offset < successorDist) {
						successorDist = testOffset - offset;
						successor = test.getPosition();
					}
				}
			}

			final int length = successor.getOffset() - precursor.getOffset();
			final StringBuffer testText = new StringBuffer(document.get(precursor.getOffset(), length));

			final Pattern pattern = Pattern.compile(
					"(//@\\.config\\n(\\s*^(?!//@).*\\n)*?\\s*markerLines=((\\d+,)*\\d+)?)", Pattern.MULTILINE);
			final Matcher matcher = pattern.matcher(testText);

			if (matcher.find()) {

				if (matcher.group(3) == null) {
					testText.insert(matcher.end(), relativeLineNo);
					additionalLength = ("" + relativeLineNo).length();
				} else {
					testText.insert(matcher.end(), "," + relativeLineNo);
					additionalLength = ("," + relativeLineNo).length();
				}

				document.replace(precursor.getOffset() + matcher.start(), matcher.end() - matcher.start(),
						testText.substring(matcher.start(), matcher.end() + additionalLength));
			}
		} catch (final IllegalStateException | BadLocationException e) {
		}
		return additionalLength;
	}

	private void setupPositionCategories() {
		final IDocument document = fProvider.getDocument(fInput);
		for (final String partition : PARTITION_TYPES) {
			document.addPositionCategory(partition);
		}
	}

	public TestFile(final FileEditorInput input, final IDocumentProvider provider, final Observer observer) {
		this(input, provider);
		addObserver(observer);
	}

	@Override
	public String toString() {
		return fName;
	}

	public Test[] getTests() {
		return fTests.toArray(new Test[0]);
	}

	public Set<Problem> getProblems() {
		return fProblems;
	}

	@Override
	public int hashCode() {
		return fName.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return hashCode() == obj.hashCode();
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

			if (lineContent.startsWith(Tokens.TEST)) {
				final Position tagPosition = new Position(lineOffset, lineLength);
				document.addPosition(PARTITION_TEST_NAME, tagPosition);
				currentTest = new Test(lineContent.substring(Tokens.TEST.length()).trim(), tagPosition, this);
				if (fTests.contains(currentTest)) {
					final Problem duplicateTest = new DuplicateTest(currentTest.toString(), currentLine + 1,
							tagPosition);
					reportProblem(duplicateTest);
					fProblems.add(new DuplicateTest(currentTest.toString(), currentLine + 1, tagPosition));
				} else {
					fTests.add(currentTest);
				}
				currentState = ParseState.TEST;
			} else if (lineContent.startsWith(Tokens.LANGUAGE) && currentTest != null) {
				final Position tagPosition = new Position(lineOffset, lineLength);
				document.addPosition(PARTITION_TEST_LANGUAGE, tagPosition);
				currentTest.setLang(
						new Language(lineContent.substring(Tokens.LANGUAGE.length()).trim(), tagPosition, currentTest));
			} else if (lineContent.startsWith(Tokens.EXPECTED)) {
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
								lineContent.substring(Tokens.EXPECTED.length()).trim(), tagPosition));
					}
					break;
				case SELECTION:
					if (currentFile != null) {
						final Position tagPosition = new Position(lineOffset, lineLength);
						document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
						currentFile.setExpected(new Expected(currentTest,
								lineContent.substring(Tokens.EXPECTED.length()).trim(), tagPosition));
					}
					break;
				default:
				}
			} else if (lineContent.startsWith(Tokens.FILE) && currentTest != null) {
				if (currentState == ParseState.SELECTION) {
					final Position tagPosition = new Position(currentSelectionStart,
							lineOffset - currentSelectionStart);
					document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
					currentFile.setSelection(new Selection(tagPosition, currentFile));
				}
				final Position tagPosition = new Position(lineOffset, lineLength);
				document.addPosition(PARTITION_TEST_FILE, tagPosition);
				currentFile = new File(lineContent.substring(Tokens.FILE.length()).trim(), tagPosition, currentTest);
				currentTest.addFile(currentFile);
				currentState = ParseState.FILE;
			} else if (lineContent.startsWith(Tokens.CLASS) && currentState == ParseState.TEST) {
				final Position tagPosition = new Position(lineOffset, lineLength);
				document.addPosition(PARTITION_TEST_CLASS, tagPosition);
				currentTest.setClassname(
						new Class(lineContent.substring(Tokens.CLASS.length()).trim(), tagPosition, currentTest));
			} else if (lineContent.contains(Tokens.SELECTION_OPEN) && currentState == ParseState.FILE) {
				currentState = ParseState.SELECTION;
				currentSelectionStart = lineOffset + lineContent.indexOf(Tokens.SELECTION_CLOSE);
			}
			if (lineContent.contains(Tokens.SELECTION_CLOSE) && currentState == ParseState.SELECTION) {
				final Position tagPosition = new Position(currentSelectionStart,
						lineOffset + lineContent.indexOf(Tokens.SELECTION_CLOSE) - currentSelectionStart);
				document.addPosition(PARTITION_TEST_SELECTION, tagPosition);
				currentFile.setSelection(new Selection(tagPosition, currentFile));
				currentState = ParseState.FILE;
			}
		}

		setChanged();
	}

	private void reportProblem(final Problem problem) throws CoreException {
		final IFile file = fInput.getFile();
		final IMarker marker = file.createMarker(MARKER_ID_DUPLICATE_TEST);
		MarkerUtilities.setCharStart(marker, problem.getPosition().offset);
		MarkerUtilities.setCharEnd(marker, problem.getPosition().offset + problem.getPosition().length);
		MarkerUtilities.setLineNumber(marker, problem.getLineNumber());
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		marker.setAttribute(IMarker.MESSAGE, problem.getDescription());
	}

	private void cleanMarkers() {
		try {
			final IMarker[] currentMarkers = fInput.getFile().findMarkers(MARKER_ID_DUPLICATE_TEST, false,
					IFile.DEPTH_INFINITE);
			for (final IMarker marker : currentMarkers) {
				final int offset = MarkerUtilities.getCharStart(marker);
				final int length = MarkerUtilities.getCharEnd(marker) - offset;
				final Position position = new Position(offset, length);

				Problem found = null;
				for (final Problem problem : fProblems) {
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
		} catch (final CoreException e) {
			Activator.logError(e, 0);
		}
	}

	private enum ParseState {
		INIT, TEST, FILE, SELECTION
	}

	public void setInput(final FileEditorInput input) {
		fInput = input;
		setupPositionCategories();
	}
}
