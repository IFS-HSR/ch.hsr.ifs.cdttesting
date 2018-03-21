package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.codan.core.model.IProblemReporter;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingTest;
import ch.hsr.ifs.cdttesting.cdttest.base.FakeProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.base.TestProjectHolder;


public abstract class CDTTestingCheckerTest extends CDTTestingTest {

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      enableChecker();
      runCodan();
   }

   @Override
   protected void initCurrentExpectedProjectHolders() throws Exception {
      /* Do not create expected project for performance reasons */
      currentProjectHolder = new TestProjectHolder(makeCurrentProjectName(), false);
      expectedProjectHolder = new FakeProjectHolder(makeExpectedProjectName());
      scheduleAndJoinBoth(currentProjectHolder.createProjectAsync(), expectedProjectHolder.createProjectAsync());
   }

   protected void assertProblemMarkerMessages(final String[] expectedMarkerMessages) throws CoreException {
      assertProblemMarkerMessages(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerMessages);
   }

   protected void assertProblemMarkerMessages(final String expectedMarkerId, final String[] expectedMarkerMessages) throws CoreException {
      final List<String> expectedList = new ArrayList<>(Arrays.asList(expectedMarkerMessages));
      final IMarker[] markers = findMarkers(expectedMarkerId);
      for (final IMarker curMarker : markers) {
         final String markerMsg = curMarker.getAttribute("message", null);
         if (expectedList.contains(markerMsg)) {
            expectedList.remove(markerMsg);
         } else {
            fail("marker-message '" + markerMsg + "' not present in given marker message list");
         }
      }
      assertTrue("Not all expected messages found. Remaining: " + expectedList, expectedList.isEmpty());
   }

   protected void assertProblemMarker(final String expectedMsg, final int expectedLine) throws CoreException {
      final IMarker[] markers = findMarkers();
      final String msg =
                       "assertProblemMarker(String, int) is only intended when there is exactly one marker. Use assertProblemMarker(String, int, IMarker) and findMarkers(...) otherwise.";
      assertEquals(msg, 1, markers.length);
      assertProblemMarker(expectedMsg, expectedLine, markers[0]);
   }

   protected void assertProblemMarker(final String expectedMsg, final int expectedLine, final IMarker marker) {
      assertEquals(expectedMsg, marker.getAttribute("message", null));
      assertEquals(expectedLine, marker.getAttribute("lineNumber", -1));
   }

   protected void assertProblemMarkerPositions(final Integer... expectedMarkerLines) throws CoreException {
      assertProblemMarkerPositions(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerLines);
   }

   protected void assertProblemMarkerPositions(final String expectedMarkerId, final Integer... expectedMarkerLines) throws CoreException {
      final List<Integer> expectedList = new ArrayList<>(Arrays.asList(expectedMarkerLines));
      final IMarker[] markers = findMarkers(expectedMarkerId);
      for (final IMarker curMarker : markers) {
         final int markerLine = curMarker.getAttribute("lineNumber", -1);
         if (expectedList.contains(markerLine)) {
            expectedList.remove((Integer) markerLine);
         } else {
            fail("marker-line '" + markerLine + "' not present in given marker lines list");
         }
      }
      assertTrue("Not all expected line numbers found. Remaining: " + expectedList, expectedList.isEmpty());
   }
}
