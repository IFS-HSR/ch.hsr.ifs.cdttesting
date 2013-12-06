package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;

public class ExampleRefactoringTest extends CDTTestingRefactoringTest {

	@Override
	protected Refactoring createRefactoring() {
		return new TestRefactoring(getActiveCElement(), selection, cproject);
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		assertRefactoringSuccess();
		// calling the following instead of assertRefactoringSuccess() will/would fail this test (because the TestRefactoring does not fail/throws exception etc.)
		// assertRefactoringFailure();
	}
}
