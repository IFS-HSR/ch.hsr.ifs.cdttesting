package ch.hsr.ifs.cdttesting.testsourcefile;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RTSFileParser {

   private static final String FAIL_INVALID_PARSE_STATE               = "Invalid parse state!";
   private static final String FAIL_MORE_THAN_ONE_SELECTION           = "More than one selection for file [%s] in test [%s]";
   private static final String FAIL_ONLY_ONE_EXPECTED_FILE_IS_ALLOWED = "More than one expected file for file [%s] in test [%s]";
   private static final String FAIL_SELECTION_NOT_CLOSED              = "Selection not closed for file [%s] in test [%s]";
   private static final String FAIL_TEST_HAS_NO_NAME                  = "Test has no name";
   private static final String FAIL_FILE_HAS_NO_NAME                  = "File in test [%s] has no name";

   public static final String CLASS           = "//#";
   public static final String COMMENT_OPEN    = "/*";
   public static final String COMMENT_CLOSE   = "*/";
   public static final String EXPECTED        = "//=";
   public static final String FILE            = "//@";
   public static final String LANGUAGE        = "//%";
   public static final String TEST            = "//!";
   public static final String SELECTION_CLOSE = "/*$";
   public static final String SELECTION_OPEN  = "$*/";

   public static final String SELECTION_START_TAG_REGEX = "(.*?)(/\\*\\$)(.*?)(\\*/)(.*)";
   public static final String SELECTION_END_TAG_REGEX   = "(.*?)(/\\*)(.*?)(\\$\\*/)(.*)";

   public static ArrayList<RTSTest> parse(final BufferedReader inputReader) throws Exception {
      final Matcher BEGIN_OF_SELECTION_MATCHER = Pattern.compile(SELECTION_START_TAG_REGEX).matcher("");
      final Matcher END_OF_SELECTION_MATCHER = Pattern.compile(SELECTION_END_TAG_REGEX).matcher("");

      final ArrayList<RTSTest> testCases = new ArrayList<>();

      RTSTest currentTest = null;
      TestSourceFile currentFile = null;

      String failMSG = FAIL_INVALID_PARSE_STATE;

      MatcherState matcherState = MatcherState.ROOT;

      /* YES CODE DUPLICATION MUTCH, BUT FUCK THAT, IT'S FAST!! */

      String line;
      while ((line = inputReader.readLine()) != null) {

         switch (matcherState) {
         case ROOT:
            if (isTEST(line)) {
               final String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  matcherState = MatcherState.IN_TEST_CASE;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            }
            break;
         case IN_TEST_CASE:
            if (isFILE(line)) {
               final String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.IN_TEST_FILE;
               }
            } else if (isTEST(line)) {
               final String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  matcherState = MatcherState.IN_TEST_CASE;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (isLANGUAGE(line)) {
               currentTest.setLanguage(getValue(LANGUAGE, line));
            }
            break;
         case IN_TEST_FILE:
            if (isFILE(line)) {
               final String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.IN_TEST_FILE;
               }
            } else if (isEXPECTED(line)) {
               currentFile.initExpectedSource();
               matcherState = MatcherState.IN_EXPECTED_FILE;
               continue;
            } else if (isTEST(line)) {
               final String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  matcherState = MatcherState.IN_TEST_CASE;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (BEGIN_OF_SELECTION_MATCHER.reset(line).find()) {
               /* Opening tag on this line */
               currentFile.setSelectionStartRelativeToNextLine(BEGIN_OF_SELECTION_MATCHER.start(2));
               line = BEGIN_OF_SELECTION_MATCHER.group(1) + BEGIN_OF_SELECTION_MATCHER.group(5);
               if (BEGIN_OF_SELECTION_MATCHER.group(3).endsWith("$")) {
                  /* Tag is opening and closing */
                  currentFile.setSelectionEndRelativeToNextLine(BEGIN_OF_SELECTION_MATCHER.start(2));
               } else if (END_OF_SELECTION_MATCHER.reset(line).find()) {
                  /* Closing tag on this line */
                  currentFile.setSelectionEndRelativeToNextLine(END_OF_SELECTION_MATCHER.start(2));
                  line = END_OF_SELECTION_MATCHER.group(1) + END_OF_SELECTION_MATCHER.group(5);
               } else {
                  /* Closing tag must be on another line */
                  matcherState = MatcherState.IN_FILE_SELECTION;
               }
               currentFile.appendLineToSource(line);
            } else {
               currentFile.appendLineToSource(line);
            }
            break;
         case IN_FILE_WITH_SELECTION:
            if (isFILE(line)) {
               final String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.IN_TEST_FILE;
               }
            } else if (isEXPECTED(line)) {
               currentFile.initExpectedSource();
               matcherState = MatcherState.IN_EXPECTED_FILE;
               continue;
            } else if (isTEST(line)) {
               final String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  matcherState = MatcherState.IN_TEST_CASE;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (BEGIN_OF_SELECTION_MATCHER.reset(line).find()) {
               failMSG = String.format(FAIL_MORE_THAN_ONE_SELECTION, currentFile.getName(), currentTest.getName());
               matcherState = MatcherState.FAIL_STATE;
            } else {
               currentFile.appendLineToSource(line);
            }
            break;
         case IN_FILE_SELECTION:
            if (isFILE(line) || isEXPECTED(line) || isTEST(line)) {
               matcherState = MatcherState.FAIL_STATE;
               failMSG = String.format(FAIL_SELECTION_NOT_CLOSED, currentFile.getName(), currentTest.getName());
            } else if (END_OF_SELECTION_MATCHER.reset(line).find()) {
               line = END_OF_SELECTION_MATCHER.group(1) + END_OF_SELECTION_MATCHER.group(5);
               currentFile.setSelectionEndRelativeToNextLine(END_OF_SELECTION_MATCHER.start(2));
               matcherState = MatcherState.IN_FILE_WITH_SELECTION;
            }
            currentFile.appendLineToSource(line);
            break;
         case IN_EXPECTED_FILE:
            if (isFILE(line)) {
               final String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.IN_TEST_FILE;
               }
            } else if (isTEST(line)) {
               final String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.FAIL_STATE;
               } else {
                  matcherState = MatcherState.IN_TEST_CASE;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (isEXPECTED(line)) {
               failMSG = String.format(FAIL_ONLY_ONE_EXPECTED_FILE_IS_ALLOWED, currentFile.getName(), currentTest.getName());
               matcherState = MatcherState.FAIL_STATE;
               continue;
            } else {
               currentFile.appendLineToExpectedSource(line);
            }
            break;
         case FAIL_STATE:
            fail(failMSG);
         }
      }
      return testCases;
   }

   private static String getValue(final String attribute, final String line) {
      return line.trim().substring(attribute.length()).trim();
   }

   private static boolean isEXPECTED(final String line) {
      return line.trim().startsWith(EXPECTED);
   }

   private static boolean isTEST(final String line) {
      return line.trim().startsWith(TEST);
   }

   private static boolean isFILE(final String line) {
      return line.trim().startsWith(FILE);
   }

   private static boolean isLANGUAGE(final String line) {
      return line.trim().startsWith(LANGUAGE);
   }

   private enum MatcherState {
      ROOT, IN_TEST_CASE, IN_TEST_FILE, IN_FILE_SELECTION, IN_FILE_WITH_SELECTION, IN_EXPECTED_FILE, FAIL_STATE
   }
}
