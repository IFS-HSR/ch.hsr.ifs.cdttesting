package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.*;

public class Expected implements Node
{
    private Test parent;
    private String text;
    private Position pos;
    
    public Expected(final Test parent, final String text, final Position pos) {
        super();
        this.parent = parent;
        this.text = text;
        this.pos = pos;
    }
    
    public Node[] getChildren() {
        return null;
    }
    
    public Node getParent() {
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
