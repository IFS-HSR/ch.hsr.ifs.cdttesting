package ch.hsr.ifs.cdttesting.cdttest;

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

@SuppressWarnings("restriction")
public abstract class CDTTestingCodanCheckerTest extends CDTTestingTest {

	protected abstract String getProblemId();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		enableChecker();
		runCodan();
	}

	private void enableChecker() {
		String acriveProblemId = getProblemId();
		IProblemProfile profile = CodanRuntime.getInstance().getCheckersRegistry().getWorkspaceProfile();
		IProblem[] problems = profile.getProblems();
		for (IProblem p : problems) {
			CodanProblem codanProblem = (CodanProblem) p;
			if (codanProblem.getId().equals(acriveProblemId)) {
				enableCodanProblem(codanProblem);
			} else {
				codanProblem.setEnabled(false);
			}
		}
		CodanRuntime.getInstance().getCheckersRegistry().updateProfile(cproject.getProject(), profile);
	}

	private void enableCodanProblem(CodanProblem codanProblem) {
		IProblemPreference preference = codanProblem.getPreference();
		if (preference instanceof RootProblemPreference) {
			RootProblemPreference rootProblemPreference = (RootProblemPreference) preference;
			rootProblemPreference.getLaunchModePreference().enableInLaunchModes(CheckerLaunchMode.RUN_ON_FULL_BUILD);
		}
		codanProblem.setEnabled(true);
	}

	protected void assertProblemMarkerMessages(String[] expectedMarkerMessages) throws CoreException {
		assertProblemMarkerMessages(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerMessages);
	}

	protected void assertProblemMarkerMessages(String expectedMarkerId, String[] expectedMarkerMessages) throws CoreException {
		List<String> expectedList = new ArrayList<>(Arrays.asList(expectedMarkerMessages));
		IMarker[] markers = findMarkers(expectedMarkerId);
		for (IMarker curMarker : markers) {
			String markerMsg = curMarker.getAttribute("message", null);
			if (expectedList.contains(markerMsg)) {
				expectedList.remove(markerMsg);
			} else {
				fail("marker-message '" + markerMsg + "' not present in given marker message list");
			}
		}
		assertTrue("Not all expected messages found. Remaining: " + expectedList, expectedList.isEmpty());
	}

	protected void assertMarkerPositions(Integer... expectedMarkerLinse) throws CoreException {
		assertMarkerPositions(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerLinse);
	}

	protected void assertMarkerPositions(String expectedMarkerId, Integer... expectedMarkerLinse) throws CoreException {
		List<Integer> expectedList = new ArrayList<>(Arrays.asList(expectedMarkerLinse));
		IMarker[] markers = findMarkers(expectedMarkerId);
		for (IMarker curMarker : markers) {
			int markerLine = curMarker.getAttribute("lineNumber", -1);
			if (expectedList.contains(markerLine)) {
				expectedList.remove((Integer) markerLine);
			} else {
				fail("marker-line '" + markerLine + "' not present in given marker lines list");
			}
		}
		assertTrue("Not all expected line numbers found. Remaining: " + expectedList, expectedList.isEmpty());
	}

	protected IMarker[] findMarkers(String markerTypeStringToFind) throws CoreException {
		return project.findMarkers(markerTypeStringToFind, true, IResource.DEPTH_INFINITE);
	}

	protected IMarker[] findMarkers() throws CoreException {
		return findMarkers(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE);
	}

	protected ICodanProblemMarker getCodanMarker(IMarker marker) {
		return CodanProblemMarker.createCodanProblemMarkerFromResourceMarker(marker);
	}

	private void runCodan() {
		CodanRuntime.getInstance().getBuilder().processResource(cproject.getProject(), new NullProgressMonitor());
	}
}
