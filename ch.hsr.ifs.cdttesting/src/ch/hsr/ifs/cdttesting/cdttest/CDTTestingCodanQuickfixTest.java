package ch.hsr.ifs.cdttesting.cdttest;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

public abstract class CDTTestingCodanQuickfixTest extends CDTTestingCodanCheckerTest {

	protected void runQuickFix(IMarkerResolution quickFix) throws CoreException {
		IMarker[] markers = findMarkers();
		String msg = "CDTTestingCodanQuickfixTest.runQuickFix(quickfix) is only intended to run on testcases containing only exactly 1 marker. "
				+ "Use overlaod runQuickFix(marker, quickfix) for other cases. Use findMarkers-methods to find available markers.";
		assertEquals(msg, 1, markers.length);
		runQuickFix(markers[0], quickFix);
	}

	protected void runQuickFix(IMarker marker, IMarkerResolution quickFix) {
		quickFix.run(marker);
	}
}
