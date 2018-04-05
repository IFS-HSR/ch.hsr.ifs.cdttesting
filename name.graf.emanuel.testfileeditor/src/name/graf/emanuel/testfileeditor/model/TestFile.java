package name.graf.emanuel.testfileeditor.model;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
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

   private static final String CONFIG_FILE_STRING       = ".config";
   private static final String MARKER_LINES_STRING      = "markerLines=";
   private static final String CONFIG_TEXT_STRING_REGEX = "(//@\\.config\\r?\\n(\\s*^(?!//@).*\\r?\\n)*?\\s*markerLines=((\\d+,)*\\d+)?)";

   private static final String MARKER_ID_GENERIC         = "name.graf.emanuel.testfileeditor.markers";
   private static final String MARKER_ID_DUPLICATE_TEST  = "name.graf.emanuel.testfileeditor.markers.DuplicateTestMarker";
   private static final String MARKER_ID_SNAKE_CASE_NAME = "name.graf.emanuel.testfileeditor.markers.SnakeCaseNameMarker";

   private final String            fName;
   private final Set<Test>         fTests;
   private final Set<Problem>      fProblems;
   private final IDocumentProvider fProvider;
   private FileEditorInput         fInput;

   public static final String PARTITION_TEST_CLASS     = "__rts_class";
   public static final String PARTITION_TEST_COMMENT   = "__rts_comment";
   public static final String PARTITION_TEST_EXPECTED  = "__rts_expected";
   public static final String PARTITION_TEST_FILE      = "__rts_file";
   public static final String PARTITION_TEST_LANGUAGE  = "__rts_language";
   public static final String PARTITION_TEST_NAME      = "__rts_name";
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
    *        lineNo Absolute line number in the file
    * @param int
    *        relativeLineNo Relative line number to the virtual file
    * @return int The length difference from before to after.
    */
   public int addLineNoToMarkerList(final int lineNo, final int relativeLineNo) {
      final IDocument document = fProvider.getDocument(fInput);
      int deltaLength = 0;

      try {
         final int offset = document.getLineOffset(lineNo);
         final Test test = getTest(offset);

         if (test == null) { return deltaLength; }

         deltaLength += addConfigFileIfNotExists(test);
         deltaLength += addMarkerLinePropertyIfNotExists(test);
         deltaLength += addVirtLineNumToMarkerLines(test, relativeLineNo);

      } catch (final IllegalStateException | BadLocationException e) {} finally {
         parse();
      }

      return deltaLength;
   }

   private Test getTest(final int offset) {
      for (final Test test : fTests) {
         if (test.containsOffset(offset)) { return test; }
      }
      return null;
   }

   private String getDocText(final Position position) throws BadLocationException {
      return fProvider.getDocument(fInput).get(position.getOffset(), position.getLength());
   }

   /*
    * @author tstauber
    * Adds the relativeLineNo to the markerLines.
    */
   private int addVirtLineNumToMarkerLines(final Test test, final int relativeLineNo) throws BadLocationException {

      final File file = test.getFile(CONFIG_FILE_STRING);
      if (file == null) { return 0; }

      final String fileText = getDocText(file.getPosition());
      final Pattern pattern = Pattern.compile(TestFile.CONFIG_TEXT_STRING_REGEX, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(fileText);

      if (matcher.find()) {
         final int offset = file.getPosition().getOffset() + matcher.end();

         String insertText = Integer.toString(relativeLineNo);
         int additionalLength = insertText.length();

         if (matcher.group(3) != null) {
            insertText = ",".concat(insertText);
            additionalLength++;
         }

         fProvider.getDocument(fInput).replace(offset, 0, insertText);

         adjustPostitionLength(file, additionalLength);
         adjustPostitionLength(test, additionalLength);

         return additionalLength;
      }
      return 0;
   }

   /*
    * @author tstauber
    * Checks if a //@.config file exists and creates one for this Test in case
    * it does not exist.
    * return additional length
    */
   private int addConfigFileIfNotExists(final Test test) throws BadLocationException {
      if (!test.containsFile(CONFIG_FILE_STRING)) {
         insertFileIntoTest(test, CONFIG_FILE_STRING, 0);
         return Tokens.FILE.concat(CONFIG_FILE_STRING).concat("\n").length();
      }
      return 0;
   }

   /*
    * @author tstauber
    * Checks if a markerLines= property exists for this Test's //@.config file
    * and creates one in case it does not exist.
    * returns the additional length
    */
   private int addMarkerLinePropertyIfNotExists(final Test test) throws BadLocationException {

      if (test.containsFile(CONFIG_FILE_STRING)) {
         final File file = test.getFile(CONFIG_FILE_STRING);
         final String fileText = getDocText(file.getPosition());

         final Pattern pattern = Pattern.compile(MARKER_LINES_STRING);
         final Matcher matcher = pattern.matcher(fileText);

         if (!matcher.find()) {
            final String insertText = MARKER_LINES_STRING.concat("\n");
            final int offset = file.getPosition().getOffset() + file.getHeadPosition().getLength();
            fProvider.getDocument(fInput).replace(offset, 0, insertText);

            final int additionalLength = insertText.length();
            adjustPostitionLength(file, additionalLength);
            adjustPostitionLength(test, additionalLength);

            return additionalLength;
         }
      }
      return 0;
   }

   /*
    * @author tstauber
    * Adjusts the size of the Test's Positions.
    */
   private static void adjustPostitionLength(final Test test, final int additionalLength) {
      test.getPosition().setLength(test.getPosition().getLength() + additionalLength);
   }

   private static void adjustPostitionLength(final File file, final int additionalLength) {
      file.getPosition().setLength(file.getPosition().getLength() + additionalLength);
   }

   /*
    * @author tstauber
    * Checks if a markerLines= property exists for this Test's //@.config file
    * and creates one in case it does not exist.
    */
   private void insertFileIntoTest(final Test test, final String name, final int offsetInTestBody) throws BadLocationException {
      final int insertOffset = test.getPosition().getOffset() + test.getHeadPosition().getLength() + offsetInTestBody;
      final String insertText = Tokens.FILE.concat(name).concat("\n");

      final Position pos = new Position(insertOffset, insertText.length());
      final Position headPos = new Position(insertOffset, insertText.length());
      test.addFile(new File(name, pos, headPos, test));

      adjustPostitionLength(test, insertText.length());

      fProvider.getDocument(fInput).replace(insertOffset, 0, insertText);
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
      Test previousTest = null;

      File currentFile = null;
      File previousFile = null;

      ParseState currentState = ParseState.INIT;
      int currentSelectionStart = 0;

      for (int currentLine = 0; currentLine < NOF_LINES; ++currentLine) {
         final int lineOffset = document.getLineOffset(currentLine);
         final int lineLength = document.getLineLength(currentLine);
         final String lineContent = document.get(lineOffset, lineLength);

         if (lineContent.length() > 3) {
            switch (lineContent.substring(0, Tokens.TOKENLENGTH)) {
            case Tokens.TEST:

               final Position headPos_TEST = new Position(lineOffset, lineLength);
               final Position pos_TEST = new Position(lineOffset, document.getLength() - lineOffset);
               document.addPosition(PARTITION_TEST_NAME, headPos_TEST);
               currentTest = new Test(lineContent.trim().substring(Tokens.TOKENLENGTH), pos_TEST, headPos_TEST, this);

               if (previousTest != null) {
                  previousTest.getPosition().setLength(lineOffset - previousTest.getPosition().getOffset());
               }
               previousTest = currentTest;

               if (isSnakeCase(currentTest.getName())) {
                  final Problem snakeCaseName = new SnakeCaseName(currentTest.getName(), currentLine + 1, headPos_TEST);
                  reportProblem(snakeCaseName, MARKER_ID_SNAKE_CASE_NAME);
                  fProblems.add(snakeCaseName);
               }
               if (fTests.contains(currentTest)) {
                  final Problem duplicateTest = new DuplicateTest(currentTest.getName(), currentLine + 1, headPos_TEST);
                  reportProblem(duplicateTest, MARKER_ID_DUPLICATE_TEST);
                  fProblems.add(duplicateTest);
               } else {
                  fTests.add(currentTest);
               }
               currentState = ParseState.TEST;

               break;

            case Tokens.LANGUAGE:
               if (previousFile != null) {
                  previousFile.getPosition().setLength(lineOffset - previousFile.getPosition().getOffset());
                  previousFile = null;
               }
               if (currentTest != null) {

                  final Position pos_LANG = new Position(lineOffset, lineLength);

                  document.addPosition(PARTITION_TEST_LANGUAGE, pos_LANG);
                  currentTest.setLang(new Language(lineContent.trim().substring(Tokens.TOKENLENGTH), pos_LANG, currentTest));

               }
               break;

            case Tokens.EXPECTED:
               if (previousFile != null) {
                  previousFile.getPosition().setLength(lineOffset - previousFile.getPosition().getOffset());
                  previousFile = null;
               }

               switch (currentState) {
               case SELECTION:
                  final Position pos_SEL_OPEN = new Position(currentSelectionStart, lineOffset - currentSelectionStart);
                  document.addPosition(PARTITION_TEST_EXPECTED, pos_SEL_OPEN);
                  currentFile.setSelection(new Selection(pos_SEL_OPEN, currentFile));
                  currentState = ParseState.FILE;
               case FILE:
                  if (currentTest != null) {
                     final Position pos_FILE = new Position(lineOffset, lineLength);
                     document.addPosition(PARTITION_TEST_EXPECTED, pos_FILE);
                     currentTest.setExpected(new Expected(currentTest, lineContent.trim().substring(Tokens.TOKENLENGTH), pos_FILE));
                  }
                  break;
               default:
               }
               break;

            case Tokens.FILE:
               if (currentTest != null) {
                  if (currentState == ParseState.SELECTION) {
                     final Position pos_SEL_OPEN = new Position(currentSelectionStart, lineOffset - currentSelectionStart);
                     document.addPosition(PARTITION_TEST_SELECTION, pos_SEL_OPEN);
                     currentFile.setSelection(new Selection(pos_SEL_OPEN, currentFile));
                  }

                  final Position headPos_FILE = new Position(lineOffset, lineLength);
                  final Position pos_FILE = new Position(lineOffset, lineLength);
                  document.addPosition(PARTITION_TEST_FILE, headPos_FILE);
                  currentFile = new File(lineContent.trim().substring(Tokens.TOKENLENGTH), pos_FILE, headPos_FILE, currentTest);

                  if (previousFile != null) {
                     previousFile.getPosition().setLength(lineOffset - previousFile.getPosition().getOffset());
                  }
                  previousFile = currentFile;

                  currentTest.addFile(currentFile);
                  currentState = ParseState.FILE;
               }
               break;

            case Tokens.CLASS:
               if (previousFile != null) {
                  previousFile.getPosition().setLength(lineOffset - previousFile.getPosition().getOffset());
                  previousFile = null;
               }

               if (currentState == ParseState.TEST) {
                  final Position pos_CLASS = new Position(lineOffset, lineLength);
                  document.addPosition(PARTITION_TEST_CLASS, pos_CLASS);
                  currentTest.setClassname(new Class(lineContent.trim().substring(Tokens.TOKENLENGTH), pos_CLASS, currentTest));
               }
               break;

            case Tokens.SELECTION_OPEN:

               if (currentState == ParseState.FILE) {
                  currentState = ParseState.SELECTION;
                  currentSelectionStart = lineOffset + lineContent.indexOf(Tokens.SELECTION_OPEN);
               }
               break;
            }
         }

         if (lineContent.contains(Tokens.SELECTION_CLOSE) && currentState == ParseState.SELECTION) {
            final Position pos_SEL_CLOSE = new Position(currentSelectionStart, lineOffset + lineContent.indexOf(Tokens.SELECTION_CLOSE) -
                                                                               currentSelectionStart);
            document.addPosition(PARTITION_TEST_SELECTION, pos_SEL_CLOSE);
            currentFile.setSelection(new Selection(pos_SEL_CLOSE, currentFile));
            currentState = ParseState.FILE;
         }
      }

      setChanged();
   }

   private boolean isSnakeCase(String testName) {
      return !testName.trim().contains(" ");
   }

   private void reportProblem(final Problem problem, String markerID) throws CoreException {
      final IFile file = fInput.getFile();
      final IMarker marker = file.createMarker(markerID);
      MarkerUtilities.setCharStart(marker, problem.getPosition().offset);
      MarkerUtilities.setCharEnd(marker, problem.getPosition().offset + problem.getPosition().length);
      MarkerUtilities.setLineNumber(marker, problem.getLineNumber());
      marker.setAttribute(IMarker.SEVERITY, problem.getSeverity());
      marker.setAttribute(IMarker.PRIORITY, problem.getPrioriry());
      marker.setAttribute(IMarker.MESSAGE, problem.getDescription());
   }

   private void cleanMarkers() {
      try {
         final IMarker[] currentMarkers = fInput.getFile().findMarkers(MARKER_ID_GENERIC, true, IResource.DEPTH_INFINITE);
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
