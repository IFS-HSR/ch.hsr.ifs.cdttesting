package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.*;

public class Class implements Node
{
    private String name;
    private Position pos;
    private Test parent;
    
    public Class(final String name, final Position pos, final Test parent) {
        super();
        this.name = name;
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
        final long namenHash = this.name.hashCode();
        return (int)namenHash + this.pos.offset;
    }
    
    public String toString() {
        int startClassName = 0;
        int tempIndex = 0;
        while ((tempIndex = this.name.indexOf(46, tempIndex + 1)) != -1) {
            startClassName = tempIndex;
        }
        return startClassName > 0 ? this.name.substring(startClassName + 1) : "";
    }
}
