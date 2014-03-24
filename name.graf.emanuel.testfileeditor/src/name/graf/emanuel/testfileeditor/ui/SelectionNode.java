package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.*;

public class SelectionNode implements ITestFileNode
{
    private Position pos;
    private FileDefNode parent;
    
    public SelectionNode(final Position pos, final FileDefNode parent) {
        super();
        this.pos = pos;
        this.parent = parent;
    }
    
    public ITestFileNode[] getChildren() {
        return null;
    }
    
    public Object getParent() {
        return this.parent;
    }
    
    public Position getPosition() {
        return this.pos;
    }
    
    public boolean hasChildren() {
        return false;
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
    
    public int hashCode() {
        return super.hashCode();
    }
    
    public String toString() {
        return "";
    }
}
