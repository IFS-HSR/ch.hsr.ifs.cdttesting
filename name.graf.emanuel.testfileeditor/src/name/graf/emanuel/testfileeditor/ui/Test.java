package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.*;
import java.util.*;

public class Test implements ITestFileNode
{
    private String name;
    private Position pos;
    private TestFile parent;
    private LanguageDef lang;
    private ExpectedNode exp;
    private ClassNameNode className;
    private Vector<FileDefNode> fileDefs;
    
    public Test(final String name, final Position pos, final TestFile file) {
        super();
        this.lang = null;
        this.exp = null;
        this.className = null;
        this.fileDefs = new Vector<FileDefNode>();
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
        final long namenHash = this.name.hashCode();
        return (int)namenHash + this.pos.offset;
    }
    
    public ITestFileNode[] getChildren() {
        final int length = this.howManyChildren();
        final ITestFileNode[] children = new ITestFileNode[length];
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
        for (final ITestFileNode node : this.fileDefs) {
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
    
    public void setLang(final LanguageDef lang) {
        this.lang = lang;
    }
    
    public void setExpected(final ExpectedNode exp) {
        this.exp = exp;
    }
    
    public void setClassname(final ClassNameNode className) {
        this.className = className;
    }
    
    public void addFile(final FileDefNode file) {
        this.fileDefs.add(file);
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
}
