package ch.hsr.ifs.cdttesting.example.examplecodantest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanQuickfixTest;


public class ExampleCodanQuickFixTest extends CDTTestingCodanQuickfixTest {

   @Override
   protected String getProblemId() {
      return MyCodanChecker.MY_PROBLEM_ID;
   }

   @Test
   public void runTest() throws Throwable {
      runQuickFix(new MyQuickFix());
      assertEquals(getExpectedSource(), getCurrentSource());
   }
}
