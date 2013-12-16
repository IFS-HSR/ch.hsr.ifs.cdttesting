package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;

public class ExampleRefactoringTest extends CDTTestingRefactoringTest {

	private TestRefactoring testRefactoring;

	@Override
	protected Refactoring createRefactoring() {
		testRefactoring = new TestRefactoring(getActiveCElement(), selection, cproject);
		return testRefactoring;
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		openActiveFileInEditor();
		assertRefactoringSuccess();
		assertTrue(testRefactoring.wasAstRunnableCalled());
		// calling the following instead of assertRefactoringSuccess() will/would fail this test (because the TestRefactoring does not fail/throws exception etc.)
		// assertRefactoringFailure();
	}
}
