package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.*;

import name.graf.emanuel.testfileeditor.model.TestFile;

import java.util.*;

public class Test implements Node
{
    private String name;
    private Position pos;
    private TestFile parent;
    private Language lang;
    private Expected exp;
    private Class className;
    private Vector<File> fileDefs;
    
    public Test(final String name, final Position pos, final TestFile file) {
        super();
        this.lang = null;
        this.exp = null;
        this.className = null;
        this.fileDefs = new Vector<File>();
        this.name = name;
        this.pos = pos;
        this.parent = file;
    }
    
    public Position getPosition() {
        return this.pos;
    }
    
    public String toString() {
        return this.name;
    }
    
    public int hashCode() {
        return this.name.hashCode();
    }
    
    public Node[] getChildren() {
        final int length = this.howManyChildren();
        final Node[] children = new Node[length];
        int index = 0;
        if (this.className != null) {
            children[index++] = this.className;
        }
        if (this.lang != null) {
            children[index++] = this.lang;
        }
        if (this.exp != null) {
            children[index++] = this.exp;
        }
        for (final Node node : this.fileDefs) {
            children[index++] = node;
        }
        return children;
    }
    
    private int howManyChildren() {
        int length = 0;
        if (this.lang != null) {
            ++length;
        }
        if (this.exp != null) {
            ++length;
        }
        if (this.className != null) {
            ++length;
        }
        length += this.fileDefs.size();
        return length;
    }
    
    public Object getParent() {
        return this.parent;
    }
    
    public boolean hasChildren() {
        return this.howManyChildren() > 0;
    }
    
    public void setLang(final Language lang) {
        this.lang = lang;
    }
    
    public void setExpected(final Expected exp) {
        this.exp = exp;
    }
    
    public void setClassname(final Class className) {
        this.className = className;
    }
    
    public void addFile(final File file) {
        this.fileDefs.add(file);
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
}
