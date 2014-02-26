package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingRefactoringTest;
import ch.hsr.ifs.cdttesting.example.examplerefactoringtest.refactorings.DummyRenameRefactoring;

public class ExampleRefactoringModificationsTest extends CDTTestingRefactoringTest {


	@Override
	protected Refactoring createRefactoring() {
		return new DummyRenameRefactoring(getActiveCElement(), selection, cproject);
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		openActiveFileInEditor();
		assertRefactoringSuccess();
	}
}
