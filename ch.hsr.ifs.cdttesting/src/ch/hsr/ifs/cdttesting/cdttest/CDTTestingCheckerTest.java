package ch.hsr.ifs.cdttesting.cdttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.codan.core.model.IProblemReporter;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.junit.Before;

import ch.hsr.ifs.iltis.core.resources.StringUtil;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingTest;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.FakeProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.ITestProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.TestProjectHolder;


/**
 * A default checker test. It runs the code analysis for the problem provided and then checks if markers appeared on all the lines declared.
 * <p>
 * An rts file for this type of test could look like this:
 * 
 * <pre>
 * {@code 
 * //! Sentence describing this test 
 * //@.config 
 * markerLines=2,4,4,5
 * //@foo.h
 * ...
 * - Line which would trigger a checker -
 * ...
 * - Line which would trigger a checker twice -
 * ...
 * - Line which would trigger a checker -
 * ....
 * }
 * </pre>
 * 
 * @author tstauber
 *
 */
public abstract class CDTTestingCheckerTest extends CDTTestingTest {

   /**
    * Contains the line numbers on which markers are expected
    */
   protected List<Integer> expectedMarkerLinesFromProperties = new LinkedList<>();

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      enableAndConfigureChecker();
      runCodeAnalysis();
   }

   @Override
   protected void initCurrentExpectedProjectHolders() throws Exception {
      currentProjectHolder = new TestProjectHolder(makeCurrentProjectName(), language, false);
      /* Create fake-expected project for performance reasons */
      expectedProjectHolder = new FakeProjectHolder(makeExpectedProjectName());
      scheduleAndJoinBoth(ITestProjectHolder::createProjectAsync);
   }

   @Override
   protected void configureTest(final Properties properties) {
      extractMarkerLines(properties);
      super.configureTest(properties);
   }

   private void extractMarkerLines(final Properties properties) {
      final String markerLines = properties.getProperty(CDTTestingConfigConstants.MARKER_LINES);
      if (markerLines != null && !markerLines.isEmpty()) {
         expectedMarkerLinesFromProperties = Stream.of(markerLines.split(",")).map(String::trim).map(Integer::valueOf).collect(Collectors.toList());
      }
   }

   /* vv ASSERTION vv */

   public void assertMarkerMessages(final String... expectedMarkerMessages) throws CoreException {
      assertMarkerMessages(Arrays.asList(expectedMarkerMessages));
   }

   public void assertMarkerMessages(final List<String> expectedMarkerMessages) throws CoreException {
      assertMarkerMessages(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerMessages);
   }

   public void assertMarkerMessages(final String expectedMarkerId, final String... expectedMarkerMessages) throws CoreException {
      assertMarkerMessages(expectedMarkerId, Arrays.asList(expectedMarkerMessages));
   }

   public void assertMarkerMessages(final String expectedMarkerId, final List<String> expectedMarkerMessages) throws CoreException {
      assertMarkerAttributes(IMarker.MESSAGE, "Marker-message '{0}' not present in given marker message list", findMarkers(expectedMarkerId),
            expectedMarkerMessages);
   }

   public void assertSingleMarker(final String expectedMsg, final int expectedLine) throws CoreException {
      final IMarker[] markers = findMarkers();
      assertEquals(
            "assertSingleMarker(String, int) is only intended to be uesed when there is exactly one marker. Use assertMarkerLineAndMessage(String, int, IMarker) and findMarkers(...) otherwise.",
            1, markers.length);
      assertMarkerLineAndMessage(expectedMsg, expectedLine, markers[0]);
   }

   public void assertMarkerLineAndMessage(final String expectedMsg, final int expectedLine, final IMarker marker) {
      assertEquals(expectedMsg, marker.getAttribute(IMarker.MESSAGE, null));
      assertEquals(expectedLine, marker.getAttribute(IMarker.LINE_NUMBER, -1));
   }

   public void assertMarkerLines(final Integer... expectedMarkerLines) throws CoreException {
      assertMarkerLines(Arrays.asList(expectedMarkerLines));
   }

   public void assertMarkerLines(final List<Integer> expectedMarkerLines) throws CoreException {
      assertMarkerLines(IProblemReporter.GENERIC_CODE_ANALYSIS_MARKER_TYPE, expectedMarkerLines);
   }

   public void assertMarkerLines(final String expectedMarkerId, final Integer... expectedMarkerLines) throws CoreException {
      assertMarkerLines(expectedMarkerId, Arrays.asList(expectedMarkerLines));
   }

   public void assertMarkerLines(final String expectedMarkerId, final List<Integer> expectedMarkerLines) throws CoreException {
      assertMarkerAttributes(IMarker.LINE_NUMBER, "Marker-line '{0}' not present in given marker lines list", findMarkers(expectedMarkerId),
            expectedMarkerLines.stream().map(String::valueOf).collect(Collectors.toList()));
   }

   /* vv ASSERTION INTERNALS vv */

   private void assertMarkerAttributes(final String attributeName, String failMessage, IMarker[] markers, List<String> expectedValues)
         throws CoreException {
      if (expectedValues == null) throw new IllegalArgumentException("Expected values were null");
      ArrayList<String> expecetedValuesMutable = new ArrayList<>(expectedValues);
      for (final IMarker curMarker : markers) {
         final String attribute = extractMarkerAttribute(attributeName, curMarker);
         if (expecetedValuesMutable.contains(attribute)) {
            expecetedValuesMutable.remove(attribute);
         } else {
            fail(NLS.bind(failMessage, attribute));
         }
      }
      assertTrue("Not all expected values found. Remaining: " + StringUtil.toString(expecetedValuesMutable), expecetedValuesMutable.isEmpty());
   }

   private String extractMarkerAttribute(final String attributeName, IMarker marker) throws CoreException {
      Object value = marker.getAttribute(attributeName);
      if (value == null) return null;
      return String.valueOf(value);
   }

}
