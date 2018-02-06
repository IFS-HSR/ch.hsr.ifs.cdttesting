package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;

public class Selection implements Node
{
    private Position pos;
    private File parent;
    
    public Selection(final Position pos, final File parent) {
        super();
        this.pos = pos;
        this.parent = parent;
    }
    
    public Node[] getChildren() {
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
