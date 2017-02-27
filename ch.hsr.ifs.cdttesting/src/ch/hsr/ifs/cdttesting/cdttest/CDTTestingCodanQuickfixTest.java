package ch.hsr.ifs.cdttesting.cdttest;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;

public abstract class CDTTestingCodanQuickfixTest extends CDTTestingCodanCheckerTest {

	protected void runQuickFix(final IMarkerResolution quickFix) throws Exception {
		final IMarker[] markers = findMarkers();
		final String msg = "CDTTestingCodanQuickfixTest.runQuickFix(quickfix) is only intended to run on testcases containing only exactly 1 marker. "
				+ "Use overlaod runQuickFix(marker, quickfix) for other cases. Use findMarkers-methods to find available markers.";
		assertEquals(msg, 1, markers.length);
		runQuickFix(markers[0], quickFix);
	}

	protected void runQuickFix(final IMarker marker, final IMarkerResolution quickFix) throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				quickFix.run(marker);
			}
		}.runSyncOnUIThread();
	}
}
