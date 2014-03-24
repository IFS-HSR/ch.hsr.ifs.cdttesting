package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.text.*;

public class LanguageDef implements ITestFileNode
{
    private String lang;
    private Position pos;
    private Test test;
    
    public LanguageDef(final String lang, final Position pos, final Test test) {
        super();
        this.lang = lang;
        this.pos = pos;
        this.test = test;
    }
    
    public ITestFileNode[] getChildren() {
        return null;
    }
    
    public ITestFileNode getParent() {
        return this.test;
    }
    
    public Position getPosition() {
        return this.pos;
    }
    
    public boolean hasChildren() {
        return false;
    }
    
    public String toString() {
        return this.lang;
    }
    
    public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
}
