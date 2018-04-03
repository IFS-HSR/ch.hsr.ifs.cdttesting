package ch.hsr.ifs.cdttesting.testsourcefile;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RTSFileParser {

   private static final String FAIL_ONLY_ONE_EXPECTED_FILE_IS_ALLOWED = "Only one expected file is allowed for file \'%s\'";
   private static final String FAIL_SELECTION_NOT_CLOSED              = "Selection not closed";
   private static final String FAIL_TEST_HAS_NO_NAME                  = "Test has no name";
   private static final String FAIL_FILE_HAS_NO_NAME                  = "File in test \'%s\' has no name";

   public static final String REMOVE = "";

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
   public static final String SELECTION_END_TAG_REGEX   = "(.*)(/\\*)(.*?)(\\$\\*/)(.*)";

   public static ArrayList<RTSTest> parse(final BufferedReader inputReader) throws Exception {
      Matcher BEGIN_OF_SELECTION_MATCHER = Pattern.compile(SELECTION_START_TAG_REGEX).matcher("");
      Matcher END_OF_SELECTION_MATCHER = Pattern.compile(SELECTION_END_TAG_REGEX).matcher("");

      ArrayList<RTSTest> testCases = new ArrayList<>();

      RTSTest currentTest = null;
      TestSourceFile currentFile = null;

      String line;

      String failMSG = "Invalid parse state!";

      MatcherState matcherState = MatcherState.skip;

      /* YES CODE DUPLICATION MUTCH, BUT FUCK THAT, IT'S FAST!! */

      while ((line = inputReader.readLine()) != null) {

         switch (matcherState) {
         case skip:
            if (isTEST(line)) {
               String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.inFail;
               } else {
                  matcherState = MatcherState.inTest;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            }
            break;
         case inTest:
            if (isFILE(line)) {
               String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.inFail;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.inFile;
               }
            } else if (isTEST(line)) {
               String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.inFail;
               } else {
                  matcherState = MatcherState.inTest;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (isLANGUAGE(line)) {
               currentTest.setLanguage(getValue(LANGUAGE, line));
            }
            break;
         case inFile:
            if (isFILE(line)) {
               String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.inFail;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.inFile;
               }
            } else if (isEXPECTED(line)) {
               currentFile.initExpectedSource();
               matcherState = MatcherState.inExpectedFile;
               continue;
            } else if (isTEST(line)) {
               String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.inFail;
               } else {
                  matcherState = MatcherState.inTest;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (BEGIN_OF_SELECTION_MATCHER.reset(line).find()) {
               /* Opening tag on this line */
               currentFile.setSelectionStart(BEGIN_OF_SELECTION_MATCHER.start(2) + currentFile.getSource().length());
               line = BEGIN_OF_SELECTION_MATCHER.group(1) + BEGIN_OF_SELECTION_MATCHER.group(5);
               if (BEGIN_OF_SELECTION_MATCHER.group(3).endsWith("$")) {
                  /* Tag is opening and closing */
                  currentFile.setSelectionEnd(BEGIN_OF_SELECTION_MATCHER.start(2) + currentFile.getSource().length());
               } else if (END_OF_SELECTION_MATCHER.reset(line).find(BEGIN_OF_SELECTION_MATCHER.end(2))) {
                  /* Closing tag on this line */
                  currentFile.setSelectionEnd(END_OF_SELECTION_MATCHER.start(2) + currentFile.getSource().length());
                  line = BEGIN_OF_SELECTION_MATCHER.group(1) + BEGIN_OF_SELECTION_MATCHER.group(5);
                  matcherState = MatcherState.inSelection;
               }
               currentFile.appendLineToSource(line);
            } else {
               currentFile.appendLineToSource(line);
            }
            break;
         case inSelection:
            if (isFILE(line) || isEXPECTED(line) || isTEST(line)) {
               matcherState = MatcherState.inFail;
               failMSG = FAIL_SELECTION_NOT_CLOSED;
            }
            if (END_OF_SELECTION_MATCHER.reset(line).find()) {
               line = END_OF_SELECTION_MATCHER.group(1) + END_OF_SELECTION_MATCHER.group(3);
               currentFile.setSelectionEnd(END_OF_SELECTION_MATCHER.start(2) + currentFile.getSource().length());
               matcherState = MatcherState.inFile;
            }
            currentFile.appendLineToSource(line);
            break;
         case inExpectedFile:
            if (isFILE(line)) {
               String name = getValue(FILE, line);
               if (name.length() == 0) {
                  failMSG = String.format(FAIL_FILE_HAS_NO_NAME, currentFile.getName());
                  matcherState = MatcherState.inFail;
               } else {
                  currentFile = new TestSourceFile(getValue(FILE, line));
                  currentTest.addFile(currentFile);
                  matcherState = MatcherState.inFile;
               }
            } else if (isTEST(line)) {
               String name = getValue(TEST, line);
               if (name.length() == 0) {
                  failMSG = FAIL_TEST_HAS_NO_NAME;
                  matcherState = MatcherState.inFail;
               } else {
                  matcherState = MatcherState.inTest;
                  currentTest = new RTSTest(name);
                  testCases.add(currentTest);
               }
            } else if (isEXPECTED(line)) {
               failMSG = String.format(FAIL_ONLY_ONE_EXPECTED_FILE_IS_ALLOWED, currentFile.getName());
               matcherState = MatcherState.inFail;
               continue;
            } else {
               currentFile.appendLineToExpectedSource(line);
            }
            break;
         case inFail:
            fail(failMSG);
         }
      }
      return testCases;
   }

   private static String getValue(String attribute, String line) {
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
      skip, inTest, inFile, inSelection, inExpectedFile, inFail
   }
}
