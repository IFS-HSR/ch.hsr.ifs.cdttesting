package ch.hsr.ifs.cdttesting.cdttest.base;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.RunWith;

import ch.hsr.ifs.cdttesting.cdttest.base.ITestProjectHolder.ReferencedProjectDescription;
import ch.hsr.ifs.cdttesting.helpers.ExtensionPointEvaluator;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


@RunWith(RtsTestSuite.class)
public abstract class RTSSourceFileTest extends SourceFileBaseTest {

   public static final String NL = System.getProperty("line.separator");

   protected ExtensionPointEvaluator evaluator = new ExtensionPointEvaluator(getClass());

   private enum MatcherState {
      skip, inTest, inSource, inExpectedResult
   }

   @Override
   protected void initExternalTestResourcesHolder() throws Exception {
      //FIXME do this by using adding the ExtensionPointEvaluator at time of Suite creation (this allows for more easy deletion of files)
      externalTestResourcesHolder = new ExternalTestResourceProjectHolder(EXTERNAL_TEST_RESOURCE_PROJECT_NAME);
      externalTestResourcesHolder.createProject();
      externalTestResourcesHolder.stageFilesForImport(evaluator.getExternalResourcesForActiveBundle());
      externalTestResourcesHolder.importFiles();
   }
   
   

   private static final String testRegex   = "//!(.*)\\s*(\\w*)*$";
   private static final String fileRegex   = "//@(.*)\\s*(\\w*)*$";
   private static final String resultRegex = "//=.*$";

   protected static Map<String, ArrayList<TestSourceFile>> createRTSTestSourceFiles(final BufferedReader inputReader) throws Exception {
      final Map<String, ArrayList<TestSourceFile>> testCases = new TreeMap<>();

      String line;
      ArrayList<TestSourceFile> files = new ArrayList<>();
      TestSourceFile actFile = null;
      MatcherState matcherState = MatcherState.skip;
      String testName = null;
      boolean beforeFirstTest = true;

      while ((line = inputReader.readLine()) != null) {

         if (lineMatchesBeginOfTest(line)) {
            if (!beforeFirstTest) {
               testCases.put(testName, files);
               files = new ArrayList<>();
               testName = null;
            }
            matcherState = MatcherState.inTest;
            testName = getNameOfTest(line);
            beforeFirstTest = false;
            continue;
         } else if (lineMatchesBeginOfResult(line)) {
            matcherState = MatcherState.inExpectedResult;
            if (actFile != null) {
               actFile.initExpectedSource();
            }
            continue;
         } else if (lineMatchesFileName(line)) {
            matcherState = MatcherState.inSource;
            actFile = new TestSourceFile(getFileName(line));
            files.add(actFile);
            continue;
         }

         switch (matcherState) {
         case skip:
         case inTest:
            break;
         case inSource:
            if (actFile != null) {
               actFile.addLineToSource(line);
            }
            break;
         case inExpectedResult:
            if (actFile != null) {
               actFile.addLineToExpectedSource(line);
            }
            break;
         }
      }
      testCases.put(testName, files);

      return testCases;
   }

   private static String getFileName(final String line) {
      final Matcher matcherBeginOfTest = createMatcherFromString(fileRegex, line);
      if (matcherBeginOfTest.find()) {
         return matcherBeginOfTest.group(1);
      } else {
         return null;
      }
   }

   private static boolean lineMatchesBeginOfTest(final String line) {
      return createMatcherFromString(testRegex, line).find();
   }

   private static boolean lineMatchesFileName(final String line) {
      return createMatcherFromString(fileRegex, line).find();
   }

   private static Matcher createMatcherFromString(final String pattern, final String line) {
      return Pattern.compile(pattern).matcher(line);
   }

   private static String getNameOfTest(final String line) {
      final Matcher matcherBeginOfTest = createMatcherFromString(testRegex, line);
      if (matcherBeginOfTest.find()) {
         return matcherBeginOfTest.group(1);
      } else {
         return "Not Named";
      }
   }

   private static boolean lineMatchesBeginOfResult(final String line) {
      return createMatcherFromString(resultRegex, line).find();
   }

   protected void stageReferencedProjectForBothProjects(final String projectName, final String rtsFileName) throws Exception {
      try (BufferedReader in = evaluator.getRtsFileReader(rtsFileName)) {
         final Map<String, ArrayList<TestSourceFile>> testCases = createRTSTestSourceFiles(in);
         if (testCases.isEmpty()) {
            throw new Exception("Failed to add referenced project. RTS file " + rtsFileName + " does not contain any test-cases.");
         } else if (testCases.size() > 1) {
            throw new Exception("RTS files + " + rtsFileName + " which represents a referenced project must only contain a single test case.");
         } else {
            stageReferencedProjectsForBothProjects(new ReferencedProjectDescription(projectName, testCases.values().iterator().next()));
         }
      }
   }

   @RTSTestCases
   public static Map<String, ArrayList<TestSourceFile>> testCases(final Class<? extends RTSSourceFileTest> testClass) throws Exception {
      final ExtensionPointEvaluator evaluator = new ExtensionPointEvaluator(testClass);
      try (BufferedReader in = evaluator.getRtsFileReader()) {
         final Map<String, ArrayList<TestSourceFile>> testCases = createRTSTestSourceFiles(in);
         return testCases;
      }
   }

}
