package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;
import ch.hsr.ifs.cdttesting.example.examplerefactoringtest.refactorings.ILTISTestRefactoring;

public class ILTISExampleRefactoringTest extends CDTTestingRefactoringTest {

	private ILTISTestRefactoring testRefactoring;

	@Override
	protected Refactoring createRefactoring() {
		testRefactoring = new ILTISTestRefactoring(getActiveCElement(), selection, cproject);
		return testRefactoring;
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		openActiveFileInEditor();
		runRefactoringAndAssertSuccess();
		assertTrue(testRefactoring.wasRefactoringSuccessful());
		// calling the following instead of assertRefactoringSuccess() will/would fail this test (because the TestRefactoring does not fail/throws exception etc.)
		// assertRefactoringFailure();
	}
}
