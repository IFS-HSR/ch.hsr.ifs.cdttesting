package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.codan.core.CodanRuntime;
import org.eclipse.cdt.codan.core.model.CheckerLaunchMode;
import org.eclipse.cdt.codan.core.model.ICodanProblemMarker;
import org.eclipse.cdt.codan.core.model.IProblem;
import org.eclipse.cdt.codan.core.model.IProblemProfile;
import org.eclipse.cdt.codan.core.model.IProblemReporter;
import org.eclipse.cdt.codan.core.param.IProblemPreference;
import org.eclipse.cdt.codan.core.param.RootProblemPreference;
import org.eclipse.cdt.codan.internal.core.model.CodanProblem;
import org.eclipse.cdt.codan.internal.core.model.CodanProblemMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;

import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


@SuppressWarnings("restriction")
public abstract class CDTTestingCodanCheckerTest extends CDTTestingTest {

   protected abstract IProblemId getProblemId();

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      enableChecker();
      runCodan();
   }

   private void enableChecker() {
      final IProblemId activeProblemId = getProblemId();
      final IProblemProfile profile = CodanRuntime.getInstance().getCheckersRegistry().getWorkspaceProfile();
      final IProblem[] problems = profile.getProblems();
      for (final IProblem p : problems) {
         final CodanProblem codanProblem = (CodanProblem) p;
         if (codanProblem.getId().equals(activeProblemId.getId())) {
            enableCodanProblem(codanProblem);
         } else {
            codanProblem.setEnabled(false);
         }
      }
      CodanRuntime.getInstance().getCheckersRegistry().updateProfile(cproject.getProject(), profile);
   }

   protected void problemPreferenceSetup(final RootProblemPreference preference) {}

   private void enableCodanProblem(final CodanProblem codanProblem) {
      final IProblemPreference preference = codanProblem.getPreference();
      if (preference instanceof RootProblemPreference) {
         final RootProblemPreference rootProblemPreference = (RootProblemPreference) preference;
         rootProblemPreference.getLaunchModePreference().enableInLaunchModes(CheckerLaunchMode.RUN_ON_FULL_BUILD);
         problemPreferenceSetup(rootProblemPreference);
      }
      codanProblem.setEnabled(true);
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

   protected IMarker[] findMarkers(final String markerTypeStringToFind) throws CoreException {
      return project.findMarkers(markerTypeStringToFind, true, IResource.DEPTH_INFINITE);
   }

   protected IMarker[] findMarkers() throws CoreException {
      return findMarkers(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE);
   }

   protected ICodanProblemMarker getCodanMarker(final IMarker marker) {
      return CodanProblemMarker.createCodanProblemMarkerFromResourceMarker(marker);
   }

   private void runCodan() {
      CodanRuntime.getInstance().getBuilder().processResource(cproject.getProject(), new NullProgressMonitor());
   }
}
