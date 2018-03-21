package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCheckerTest;
import ch.hsr.ifs.cdttesting.example.examplecodantest.MyCodanChecker.MyProblemId;
import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


public class ExampleCodanCheckerTest extends CDTTestingCheckerTest {

   @Override
   protected IProblemId getProblemId() {
      return MyProblemId.EXAMPLE_ID;
   }

   @Test
   public void runTest() throws Throwable {
      int markerExpectedOnLine = 1;
      assertProblemMarkerPositions(markerExpectedOnLine);
      assertProblemMarkerMessages(new String[] { "Declaration 'main' is wrong." });
   }
}
