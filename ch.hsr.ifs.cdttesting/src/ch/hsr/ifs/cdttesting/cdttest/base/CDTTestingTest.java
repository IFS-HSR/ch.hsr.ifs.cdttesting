package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.stream.Stream;

import org.eclipse.cdt.codan.core.CodanRuntime;
import org.eclipse.cdt.codan.core.model.CheckerLaunchMode;
import org.eclipse.cdt.codan.core.model.IProblemProfile;
import org.eclipse.cdt.codan.core.model.IProblemReporter;
import org.eclipse.cdt.codan.core.param.IProblemPreference;
import org.eclipse.cdt.codan.core.param.RootProblemPreference;
import org.eclipse.cdt.codan.internal.core.model.CodanProblem;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


@SuppressWarnings("restriction")
public abstract class CDTTestingTest extends RTSSourceFileTest {

   protected boolean checkerEnabled = false;
   protected boolean codanRun       = false;

   protected IProblemId getProblemId() {
      fail("Checker can not be enabled without a problem id. Overload getProblemId() propperly.");
      return null;
   }

   /**
    * Call to enable only the checker for the problem id provided in {@link #getProblemId()}
    */
   protected void enableAndConfigureChecker() {
      IProblemId activeProblemId = getProblemId();
      final IProblemProfile profile = CodanRuntime.getInstance().getCheckersRegistry().getResourceProfile(getCurrentProject());
      Stream.of(profile.getProblems()).forEach(problem -> {
         final CodanProblem codanProblem = (CodanProblem) problem;
         if (codanProblem.getId().equals(activeProblemId.getId())) {
            enableCodanProblem(codanProblem);
            checkerEnabled = true;
         } else {
            codanProblem.setEnabled(false);
         }
      });
      CodanRuntime.getInstance().getCheckersRegistry().updateProfile(getCurrentProject(), profile);
      assertTrue(NLS.bind("No checker for problem id [{0}] found.", activeProblemId.getId()), checkerEnabled);
   }

   private void enableCodanProblem(final CodanProblem codanProblem) {
      final IProblemPreference preference = codanProblem.getPreference();
      if (preference instanceof RootProblemPreference) {
         final RootProblemPreference rootProblemPreference = (RootProblemPreference) preference;
         rootProblemPreference.getLaunchModePreference().enableInLaunchModes(CheckerLaunchMode.RUN_ON_FULL_BUILD);
         problemPreferenceSetup(rootProblemPreference);
      }
      codanProblem.setEnabled(true);
   }

   protected void problemPreferenceSetup(final RootProblemPreference preference) {}

   /**
    * Finds markers for the type provided.
    * 
    * @param markerTypeStringToFind
    *        The super-type id of the marker to search for.
    * @return The markers of the type or any sub-type.
    * @throws CoreException
    */
   protected IMarker[] findMarkers(final String markerTypeStringToFind) throws CoreException {
      assertThatCodeWasAnalyzed();
      return getCurrentProject().findMarkers(markerTypeStringToFind, true, IResource.DEPTH_INFINITE);
   }

   /**
    * Finds generic codan-markers and its sub-types.
    * 
    * @return The markers
    * @throws CoreException
    */
   protected IMarker[] findMarkers() throws CoreException {
      return findMarkers(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE);
   }

   /**
    * Call to run the code analysis
    * <p>
    * {@link #enableAndConfigureChecker()} must be called before calling this method
    */
   protected void runCodeAnalysis() {
      assertThatCheckerIsEnabled();
      CodanRuntime.getInstance().getBuilder().processResource(getCurrentProject(), new NullProgressMonitor());
      codanRun = true;
   }

   protected void assertThatCodeWasAnalyzed() {
      assertTrue("Code analysis was never executed", codanRun);
   }

   protected void assertThatCheckerIsEnabled() {
      assertTrue("Checker was never enabled", checkerEnabled);
   }
}
