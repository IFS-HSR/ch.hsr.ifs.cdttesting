package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import static org.junit.Assert.assertTrue;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;
import ch.hsr.ifs.cdttesting.example.examplerefactoringtest.refactorings.TestRefactoring;


public class ExampleRefactoringTest extends CDTTestingRefactoringTest {

   private TestRefactoring testRefactoring;

   @Override
   protected Refactoring createRefactoring() {
      testRefactoring = new TestRefactoring(getCurrentCElement("XY.cpp").get(), getSelection("XY.cpp"), getCurrentCProject());
      return testRefactoring;
   }

   @Test
   public void runTest() throws Throwable {
      openTestFileInEditor("XY.cpp");
      runRefactoringAndAssertSuccess();
      assertTrue(testRefactoring.wasRefactoringSuccessful());
      // calling the following instead of assertRefactoringSuccess() will/would fail
      // this test (because the TestRefactoring does not fail/throws exception etc.)
      // assertRefactoringFailure();
   }

}
