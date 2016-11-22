package name.graf.emanuel.testfileeditor.model;

import org.eclipse.jface.text.Position;

public class DuplicateTest implements Problem {

    private final String fTestName;
    private final int fLineNumber;
    private final Position fPosition;

    DuplicateTest(String testName, int lineNumber, Position position) {
        fTestName = testName;
        fLineNumber = lineNumber;
        fPosition = position;
    }

    @Override
    public int getLineNumber() {
        return fLineNumber;
    }

    @Override
    public Position getPosition() {
        return fPosition;
    }

    @Override
    public String getDescription() {
        return "Redefinition of test '" + fTestName + "'";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof DuplicateTest)) {
            return false;
        } else {
            DuplicateTest other = (DuplicateTest) obj;
            return this.fTestName.equals(other.fTestName) && this.fPosition.equals(other.fPosition);
        }
    }
    
    @Override
    public String toString() {
        return getDescription() + "@" + fPosition.toString();
    }
    
    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
