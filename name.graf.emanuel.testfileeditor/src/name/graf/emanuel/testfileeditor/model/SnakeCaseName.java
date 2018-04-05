package name.graf.emanuel.testfileeditor.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Position;


public class SnakeCaseName extends AbstractProblem implements Problem {

   SnakeCaseName(String testName, int lineNumber, Position position) {
      super(testName, lineNumber, position);
   }

   @Override
   public String getDescription() {
      return "Test name is not a sentence (maybe snake-case or camel-case)";
   }

   @Override
   public int getPrioriry() {
      return IMarker.PRIORITY_NORMAL;
   }

   @Override
   public int getSeverity() {
      return IMarker.SEVERITY_WARNING;
   }

}
