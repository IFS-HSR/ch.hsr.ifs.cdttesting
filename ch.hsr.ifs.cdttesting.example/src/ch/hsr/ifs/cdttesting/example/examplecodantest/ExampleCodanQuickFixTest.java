package ch.hsr.ifs.cdttesting.example.examplecodantest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanQuickfixTest;
import ch.hsr.ifs.cdttesting.example.examplecodantest.MyCodanChecker.MyProblemId;
import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


public class ExampleCodanQuickFixTest extends CDTTestingCodanQuickfixTest {

   @Override
   protected IProblemId getProblemId() {
      return MyProblemId.EXAMPLE_ID;
   }

   @Test
   public void runTest() throws Throwable {
      runQuickFix(new MyQuickFix());
      assertEquals(getExpectedSource(), getCurrentSource());
   }
}
