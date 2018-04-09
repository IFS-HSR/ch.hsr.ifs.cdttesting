package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;
import ch.hsr.ifs.cdttesting.example.examplerefactoringtest.refactorings.ILTISDummyRenameRefactoring;


public class ILTISExampleRefactoringModificationsTest extends CDTTestingRefactoringTest {

   @Override
   protected Refactoring createRefactoring() {
      return new ILTISDummyRenameRefactoring(getPrimaryCElementFromCurrentProject().get(), getSelectionOfPrimaryTestFile(),
            getCurrentCProject());
   }

   @Test
   public void runTest() throws Throwable {
      openPrimaryTestFileInEditor();
      runRefactoringAndAssertSuccess();
   }
}
