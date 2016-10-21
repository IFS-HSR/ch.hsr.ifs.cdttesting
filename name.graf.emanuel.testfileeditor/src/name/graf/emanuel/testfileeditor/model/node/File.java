package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.*;

public class File implements Node
{
    private String name;
    private Position pos;
    private Test parent;
    private Expected exp;
    private Selection sel;
    
    public File(final String name, final Position pos, final Test parent) {
        super();
        this.exp = null;
        this.sel = null;
        this.name = name;
        this.pos = pos;
        this.parent = parent;
    }
    
    public Node[] getChildren() {
        final int i = this.howManyChildren();
        if (i > 0) {
            int index = 0;
            final Node[] children = new Node[i];
            if (this.sel != null) {
                children[index++] = this.sel;
            }
            if (this.exp != null) {
                children[index++] = this.exp;
            }
            return children;
        }
        return null;
    }
    
    public Object getParent() {
        return this.parent;
    }
    
    public Position getPosition() {
        return this.pos;
    }
    
    public boolean hasChildren() {
        return this.howManyChildren() > 0;
    }
    
    public String toString() {
        return this.name;
    }
    
    public int hashCode() {
        final long namenHash = this.name.hashCode();
        return (int)namenHash + this.pos.offset;
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
    
    public void setExpected(final Expected node) {
        this.exp = node;
    }
    
    public void setSelection(final Selection node) {
        this.sel = node;
    }
    
    private int howManyChildren() {
        int length = 0;
        if (this.sel != null) {
            ++length;
        }
        if (this.exp != null) {
            ++length;
        }
        return length;
    }
}
