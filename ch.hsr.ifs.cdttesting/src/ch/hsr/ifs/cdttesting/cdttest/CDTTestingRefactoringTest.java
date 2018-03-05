package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;
import ch.hsr.ifs.iltis.cpp.wrappers.CRefactoring;
import ch.hsr.ifs.iltis.cpp.wrappers.CRefactoringContext;


/**
 * Most of the code for this class originates from CDT's RefactoringTestBase
 * class. In our case, it executes on the more correctly set-up project/index of
 * our cdttesting framework
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
   protected void simulateUserInput(final RefactoringContext context) {
      simulateUserInput(); // call deprecated method if not overwritten by user
   }

   /**
    * Subclasses can override to simulate user input.
    *
    * @deprecated use {@link #simulateUserInput(RefactoringContext)} instead.
    */
   @Deprecated
   protected void simulateUserInput() {}

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
    * @deprecated use {@link #runRefactoringAndAssertSuccess()
    *             runRefactoringAndAssertSuccess} instead.
    */
   @Deprecated
   protected void assertRefactoringSuccess() throws Exception {
      runRefactoringAndAssertSuccess();
   }

   /**
    * Deprecated due to bad method name.
    *
    * @deprecated use {@link #runRefactoringAndAssertFailure()
    *             runRefactoringAndAssertFailure} instead.
    */
   @Deprecated
   protected void assertRefactoringFailure() throws Exception {
      runRefactoringAndAssertFailure();
   }

   protected void executeRefactoring(final boolean expectedSuccess) throws Exception {
      final Refactoring refactoring = createRefactoring();
      RefactoringContext context;
      if (refactoring instanceof CRefactoring) {
         context = new CRefactoringContext((CRefactoring) refactoring);
      } else if (refactoring instanceof org.eclipse.cdt.internal.ui.refactoring.CRefactoring) {
         context = new org.eclipse.cdt.internal.ui.refactoring.CRefactoringContext(
               (org.eclipse.cdt.internal.ui.refactoring.CRefactoring) refactoring);
      } else {
         context = new RefactoringContext(refactoring);
      }
      executeRefactoring(refactoring, context, true, expectedSuccess);
   }

   protected void executeRefactoring(final Refactoring refactoring, final RefactoringContext context, final boolean withUserInput,
         final boolean expectedSuccess) throws CoreException, Exception {
      try {
         final RefactoringStatus initialStatus = refactoring.checkInitialConditions(new NullProgressMonitor());
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

         if (withUserInput) {
            simulateUserInput(context);
         }
         final RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
         if (expectedFinalErrors != 0) {
            assertStatusError(finalStatus, expectedFinalErrors);
         } else if (expectedFinalWarnings != 0) {
            assertStatusWarning(finalStatus, expectedFinalWarnings);
         } else if (expectedFinalInfos != 0) {
            assertStatusInfo(finalStatus, expectedFinalInfos);
         } else {
            assertStatusOk(finalStatus);
         }
         final Change change = refactoring.createChange(new NullProgressMonitor());
         change.perform(new NullProgressMonitor());
      } finally {
         if (context != null) {
            context.dispose();
         }
      }
   }

   protected void compareFiles() throws Exception {
      for (final TestSourceFile testFile : fileMap.values()) {
         fastAssertEquals(testFile.getName(), ComparisonArg.emptySet());
      }
   }

   protected void assertStatusOk(final RefactoringStatus status) {
      if (!status.isOK()) {
         fail("Error or warning status: " + status.getEntries()[0].getMessage());
      }
   }

   protected void assertStatusWarning(final RefactoringStatus status, final int number) {
      if (number > 0) {
         assertTrue("Warning status expected", status.hasWarning());
      }
      final RefactoringStatusEntry[] entries = status.getEntries();
      int count = 0;
      for (final RefactoringStatusEntry entry : entries) {
         if (entry.isWarning()) {
            ++count;
         }
      }
      assertEquals("Found " + count + " warnings instead of expected " + number, number, count);
   }

   protected void assertStatusInfo(final RefactoringStatus status, final int number) {
      if (number > 0) {
         assertTrue("Info status expected", status.hasInfo());
      }
      final RefactoringStatusEntry[] entries = status.getEntries();
      int count = 0;
      for (final RefactoringStatusEntry entry : entries) {
         if (entry.isInfo()) {
            ++count;
         }
      }
      assertEquals("Found " + count + " informational messages instead of expected " + number, number, count);
   }

   protected void assertStatusError(final RefactoringStatus status, final int number) {
      if (number > 0) {
         assertTrue("Error status expected", status.hasError());
      }
      final RefactoringStatusEntry[] entries = status.getEntries();
      int count = 0;
      for (final RefactoringStatusEntry entry : entries) {
         if (entry.isError()) {
            ++count;
         }
      }
      assertEquals("Found " + count + " errors instead of expected " + number, number, count);
   }

   protected void assertStatusFatalError(final RefactoringStatus status, final int number) {
      if (number > 0) {
         assertTrue("Fatal error status expected", status.hasFatalError());
      }
      final RefactoringStatusEntry[] entries = status.getEntries();
      int count = 0;
      for (final RefactoringStatusEntry entry : entries) {
         if (entry.isFatalError()) {
            ++count;
         }
      }
      assertEquals("Found " + count + " fatal errors instead of expected " + number, number, count);
   }

   protected void assertStatusFatalError(final RefactoringStatus status) {
      assertTrue("Fatal error status expected", status.hasFatalError());
   }

   protected URI getActiveFileUri() {
      final String absoluteFilePath = makeProjectAbsolutePath(activeFileName, currentProject);
      return new File(absoluteFilePath).toURI();
   }

   public ICElement getActiveCElement() {
      return CoreModel.getDefault().create(getIFile(activeFileName));
   }
}
