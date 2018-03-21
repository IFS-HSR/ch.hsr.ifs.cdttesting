package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.EnumSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.junit.Before;

import ch.hsr.ifs.iltis.core.exception.ILTISException;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;
import ch.hsr.ifs.cdttesting.cdttest.base.SourceFileBaseTest;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;


public abstract class CDTTestingQuickfixTest extends CDTTestingUITest {

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      enableChecker();
      runCodan();
   }

   /**
    * Subclasses must to provide marker resolution to execute
    */
   protected abstract IMarkerResolution createMarkerResolution();

   protected void runQuickFix() throws Exception {
      final IMarker[] markers = findMarkers();
      assertOnlyOneMarker(markers);
      runQuickFix(markers[0]);
   }

   protected void runQuickFix(final IMarker marker) throws RuntimeException {
      ILTISException.Unless.notNull(marker, "Marker was null. Could not run quick fix for it.");
      new UIThreadSyncRunnable() {

         @Override
         protected void runSave() throws RuntimeException {
            createMarkerResolution().run(marker);
         }
      }.runSyncOnUIThread();
   }

   protected void runQuickfixAndAssertAllEqual(final IMarker marker) throws Exception {
      runQuickFix(marker);
      saveAllEditors();
      assertAllSourceFilesEqual(makeComparisonArguments());
   }

   protected void runQuickfixForAllMarkersAndAssertAllEqual() throws Exception {
      Arrays.stream(findMarkers()).forEach(marker -> runQuickFix(marker));
      saveAllEditors();
      assertAllSourceFilesEqual(makeComparisonArguments());
   }

   protected void runQuickfixAndAssertAllEqual() throws Exception {
      final IMarker[] markers = findMarkers();
      assertOnlyOneMarker(markers);
      runQuickFix(markers[0]);
      saveAllEditors();
      assertAllSourceFilesEqual(makeComparisonArguments());
   }

   /**
    * Can be overloaded to use specific comparison arguments in {@link #runQuickfixAndAssertAllEqual(IMarkerResolution)},
    * {@link #runQuickfixAndAssertAllEqual(IMarker, IMarkerResolution)}, and {@link #runQuickfixForAllMarkersAndAssertAllEqual(IMarkerResolution)}.
    * <p>
    * By default this uses the values stored in {@link SourceFileBaseTest#COMPARE_AST_AND_COMMENTS_AND_INCLUDES}
    * 
    * @return
    */
   protected EnumSet<ComparisonArg> makeComparisonArguments() {
      return COMPARE_AST_AND_COMMENTS_AND_INCLUDES;
   }

   private void assertOnlyOneMarker(IMarker[] markers) {
      final String msg = "CDTTestingCodanQuickfixTest.runQuickFix(quickfix) is only intended to run on testcases containing only exactly 1 marker. " +
                         "Use overlaod runQuickFix(marker, quickfix) for other cases. Use findMarkers-methods to find available markers.";
      assertEquals(msg, 1, markers.length);
   }

}
