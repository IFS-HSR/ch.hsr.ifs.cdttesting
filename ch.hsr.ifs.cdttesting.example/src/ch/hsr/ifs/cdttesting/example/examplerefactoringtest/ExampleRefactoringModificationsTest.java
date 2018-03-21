package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;
import ch.hsr.ifs.cdttesting.example.examplerefactoringtest.refactorings.DummyRenameRefactoring;


public class ExampleRefactoringModificationsTest extends CDTTestingRefactoringTest {

   private String testSourceFileName;

   @Override
   protected Refactoring createRefactoring() {
      testSourceFileName = "main.cpp";
      return new DummyRenameRefactoring(getCurrentCElement(getCurrentIFile(testSourceFileName)).get(), getSelection(testSourceFileName),
            getCurrentCProject());
   }

   @Test
   public void runTest() throws Throwable {
      openTestFileInEditor(testSourceFileName);
      runRefactoringAndAssertSuccess();
   }

}
