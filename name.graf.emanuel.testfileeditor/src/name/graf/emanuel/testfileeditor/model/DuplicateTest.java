package name.graf.emanuel.testfileeditor.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Position;


public class DuplicateTest extends AbstractProblem implements Problem {

   DuplicateTest(String testName, int lineNumber, Position position) {
      super(testName, lineNumber, position);
   }

   @Override
   public String getDescription() {
      return "Redefinition of test '" + fTestName + "'";
   }

   @Override
   public int getPrioriry() {
      return IMarker.PRIORITY_HIGH;
   }

   @Override
   public int getSeverity() {
      return IMarker.SEVERITY_ERROR;
   }
}
