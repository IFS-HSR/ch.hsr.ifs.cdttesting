package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.*;

public class ExpectedNode implements ITestFileNode
{
    private Test parent;
    private String text;
    private Position pos;
    
    public ExpectedNode(final Test parent, final String text, final Position pos) {
        super();
        this.parent = parent;
        this.text = text;
        this.pos = pos;
    }
    
    public ITestFileNode[] getChildren() {
        return null;
    }
    
    public ITestFileNode getParent() {
        return this.parent;
    }
    
    public Position getPosition() {
        return this.pos;
    }
    
    public boolean hasChildren() {
        return false;
    }
    
    public String toString() {
        return this.text;
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
}
