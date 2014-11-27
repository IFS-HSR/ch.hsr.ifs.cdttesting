package ch.hsr.ifs.cdttesting.cdttest;

import java.io.File;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;

/**
 * Most of the code for this class originates from CDT's RefactoringTestBase class. In our case, it executes on the more correctly set-up project/index of our cdttesting framework
 */
@SuppressWarnings("restriction")
public abstract class CDTTestingRefactoringTest extends CDTTestingTest {

	/** Expected counts of errors, warnings and info messages */
	protected int expectedInitialErrors;
	protected int expectedInitialWarnings;
	protected int expectedFinalErrors;
	protected int expectedFinalWarnings;
	protected int expectedFinalInfos;

	/**
	 * Subclasses must to provide refactoring to execute
	 */
	protected abstract Refactoring createRefactoring();

	/**
	 * Subclasses can override to simulate user input.
	 * 
	 * @param context
	 */
	protected void simulateUserInput(RefactoringContext context) {
		simulateUserInput(); // call deprecated method if not overwritten by user
	}

	/**
	 * Subclasses can override to simulate user input.
	 * 
	 * @deprecated use {@link #simulateUserInput(RefactoringContext)} instead.
	 */
	@Deprecated
	protected void simulateUserInput() {
	}

	protected void runRefactoringAndAssertSuccess() throws Exception {
		executeRefactoring(true);
		compareFiles();
	}

	protected void runRefactoringAndAssertFailure() throws Exception {
		executeRefactoring(false);
	}

	/**
	 * Deprecated due to bad method name.
	 * 
	 * @deprecated use {@link #runRefactoringAndAssertSuccess() runRefactoringAndAssertSuccess} instead.
	 */
	@Deprecated
	protected void assertRefactoringSuccess() throws Exception {
		runRefactoringAndAssertSuccess();
	}

	/**
	 * Deprecated due to bad method name.
	 * 
	 * @deprecated use {@link #runRefactoringAndAssertFailure() runRefactoringAndAssertFailure} instead.
	 */
	@Deprecated
	protected void assertRefactoringFailure() throws Exception {
		runRefactoringAndAssertFailure();
	}

	protected void executeRefactoring(boolean expectedSuccess) throws Exception {
		Refactoring refactoring = createRefactoring();
		RefactoringContext context;
		if (refactoring instanceof CRefactoring) {
			context = new CRefactoringContext((CRefactoring) refactoring);
		} else {
			context = new RefactoringContext(refactoring);
		}
		executeRefactoring(refactoring, context, true, expectedSuccess);
	}

	protected void executeRefactoring(Refactoring refactoring, RefactoringContext context, boolean withUserInput, boolean expectedSuccess) throws CoreException, Exception {
		try {
			RefactoringStatus initialStatus = refactoring.checkInitialConditions(new NullProgressMonitor());
			if (!expectedSuccess) {
				assertStatusFatalError(initialStatus);
				return;
			}
			if (expectedInitialErrors != 0) {
				assertStatusError(initialStatus, expectedInitialErrors);
			} else if (expectedInitialWarnings != 0) {
				assertStatusWarning(initialStatus, expectedInitialWarnings);
			} else {
				assertStatusOk(initialStatus);
			}

			if (withUserInput)
				simulateUserInput(context);
			RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
			if (expectedFinalErrors != 0) {
				assertStatusError(finalStatus, expectedFinalErrors);
			} else if (expectedFinalWarnings != 0) {
				assertStatusWarning(finalStatus, expectedFinalWarnings);
			} else if (expectedFinalInfos != 0) {
				assertStatusInfo(finalStatus, expectedFinalInfos);
			} else {
				assertStatusOk(finalStatus);
			}
			Change change = refactoring.createChange(new NullProgressMonitor());
			change.perform(new NullProgressMonitor());
		} finally {
			if (context != null)
				context.dispose();
		}
	}

	protected void compareFiles() throws Exception {
		for (TestSourceFile testFile : fileMap.values()) {
			String expectedSource = testFile.getExpectedSource();
			String actualSource = getCurrentSource(testFile.getName());
			assertEquals(expectedSource, actualSource);
		}
	}

	protected void assertStatusOk(RefactoringStatus status) {
		if (!status.isOK())
			fail("Error or warning status: " + status.getEntries()[0].getMessage());
	}

	protected void assertStatusWarning(RefactoringStatus status, int number) {
		if (number > 0) {
			assertTrue("Warning status expected", status.hasWarning());
		}
		RefactoringStatusEntry[] entries = status.getEntries();
		int count = 0;
		for (RefactoringStatusEntry entry : entries) {
			if (entry.isWarning()) {
				++count;
			}
		}
		assertEquals("Found " + count + " warnings instead of expected " + number, number, count);
	}

	protected void assertStatusInfo(RefactoringStatus status, int number) {
		if (number > 0) {
			assertTrue("Info status expected", status.hasInfo());
		}
		RefactoringStatusEntry[] entries = status.getEntries();
		int count = 0;
		for (RefactoringStatusEntry entry : entries) {
			if (entry.isInfo()) {
				++count;
			}
		}
		assertEquals("Found " + count + " informational messages instead of expected " + number, number, count);
	}

	protected void assertStatusError(RefactoringStatus status, int number) {
		if (number > 0) {
			assertTrue("Error status expected", status.hasError());
		}
		RefactoringStatusEntry[] entries = status.getEntries();
		int count = 0;
		for (RefactoringStatusEntry entry : entries) {
			if (entry.isError()) {
				++count;
			}
		}
		assertEquals("Found " + count + " errors instead of expected " + number, number, count);
	}

	protected void assertStatusFatalError(RefactoringStatus status, int number) {
		if (number > 0) {
			assertTrue("Fatal error status expected", status.hasFatalError());
		}
		RefactoringStatusEntry[] entries = status.getEntries();
		int count = 0;
		for (RefactoringStatusEntry entry : entries) {
			if (entry.isFatalError()) {
				++count;
			}
		}
		assertEquals("Found " + count + " fatal errors instead of expected " + number, number, count);
	}

	protected void assertStatusFatalError(RefactoringStatus status) {
		assertTrue("Fatal error status expected", status.hasFatalError());
	}

	protected URI getActiveFileUri() {
		String absoluteFilePath = makeProjectAbsolutePath(activeFileName);
		return new File(absoluteFilePath).toURI();
	}

	public ICElement getActiveCElement() {
		return CoreModel.getDefault().create(getIFile(activeFileName));
	}
}
