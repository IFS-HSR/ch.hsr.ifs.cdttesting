package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.util.ArrayList;

import org.junit.runner.RunWith;

import ch.hsr.ifs.cdttesting.cdttest.base.ITestProjectHolder.ReferencedProjectDescription;
import ch.hsr.ifs.cdttesting.helpers.ExtensionPointEvaluator;
import ch.hsr.ifs.cdttesting.rts.junit4.RTSTestCases;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsTestSuite;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSFileParser;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest;


@RunWith(RtsTestSuite.class)
public abstract class RTSSourceFileTest extends SourceFileBaseTest {

   protected ExtensionPointEvaluator evaluator = new ExtensionPointEvaluator(getClass());

   @Override
   protected void initExternalTestResourcesHolder() throws Exception {
      externalTestResourcesHolder = new ExternalTestResourceProjectHolder(EXTERNAL_TEST_RESOURCE_PROJECT_NAME, language);
      externalTestResourcesHolder.createProject();
      externalTestResourcesHolder.stageFilesForImport(evaluator.getExternalResourcesForActiveBundle());
      externalTestResourcesHolder.importFiles();
   }

   /**
    * Stages the file extracted from the rts file for both projects
    * 
    * @param projectName
    *        The name of the referenced project that will be created
    * @param rtsFileName
    *        The name of the rts file from which to extract the files to to the referenced project once it will be created
    * @throws Exception
    */
   protected void stageReferencedProjectForBothProjects(final String projectName, final String rtsFileName) throws Exception {
      try (BufferedReader in = evaluator.getRtsFileReader(rtsFileName)) {
         final ArrayList<RTSTest> testCases = RTSFileParser.parse(in);
         assertTrue("The RTS file + \'" + rtsFileName + "\' which represents a referenced project must contain exactly one test case.", testCases
               .size() == 1);
         stageReferencedProjectsForBothProjects(new ReferencedProjectDescription(projectName, testCases.get(0).getTestSourceFiles()));
      }
   }

   @RTSTestCases
   public static ArrayList<RTSTest> testCases(final Class<? extends RTSSourceFileTest> testClass) throws Exception {
      try (BufferedReader in = new ExtensionPointEvaluator(testClass).getRtsFileReader()) {
         return RTSFileParser.parse(in);
      }
   }

}
