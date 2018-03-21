package ch.hsr.ifs.cdttesting.cdttest.base;

import java.util.Arrays;

import org.eclipse.cdt.codan.core.CodanRuntime;
import org.eclipse.cdt.codan.core.model.CheckerLaunchMode;
import org.eclipse.cdt.codan.core.model.ICheckersRegistry;
import org.eclipse.cdt.codan.core.model.ICodanProblemMarker;
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

import ch.hsr.ifs.iltis.core.exception.ILTISException;

import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


@SuppressWarnings("restriction")
public abstract class CDTTestingTest extends RTSSourceFileTest {

   boolean checkerEnabled = false;

   protected IProblemId getProblemId() {
      return null;
   }

   /**
    * Call to enable only the checker for the problem id provided
    */
   protected void enableChecker() {
      IProblemId activeProblemId = getProblemId();
      ILTISException.Unless.notNull(activeProblemId, "Checker can not be enabled for null. Overload getProblemId propperly.");
      ICheckersRegistry checkersRegistry = CodanRuntime.getInstance().getCheckersRegistry();
      final IProblemProfile profile = checkersRegistry.getWorkspaceProfile();
      Arrays.stream(profile.getProblems()).forEach(problem -> {
         final CodanProblem codanProblem = (CodanProblem) problem;
         if (codanProblem.getId().equals(activeProblemId.getId())) {
            enableCodanProblem(codanProblem);
         } else {
            codanProblem.setEnabled(false);
         }
      });
      checkersRegistry.updateProfile(currentProjectHolder.getProject(), profile);
      checkerEnabled = true;
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

   protected IMarker[] findMarkers(final String markerTypeStringToFind) throws CoreException {
      ILTISException.Unless.isTrue(checkerEnabled, "Checker was never enabled");
      return getCurrentProject().findMarkers(markerTypeStringToFind, true, IResource.DEPTH_INFINITE);
   }

   protected IMarker[] findMarkers() throws CoreException {
      return findMarkers(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE);
   }

   protected ICodanProblemMarker getCodanMarker(final IMarker marker) {
      return CodanProblemMarker.createCodanProblemMarkerFromResourceMarker(marker);
   }

   /**
    * Call to run code analysis
    */
   protected void runCodan() {
      ILTISException.Unless.isTrue(checkerEnabled, "Checker was never enabled");
      CodanRuntime.getInstance().getBuilder().processResource(getCurrentProject(), new NullProgressMonitor());
   }
}
