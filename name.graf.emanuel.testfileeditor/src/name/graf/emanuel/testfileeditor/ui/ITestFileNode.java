package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.*;

public interface ITestFileNode
{
    Position getPosition();
    
    boolean hasChildren();
    
    ITestFileNode[] getChildren();
    
    Object getParent();
}
