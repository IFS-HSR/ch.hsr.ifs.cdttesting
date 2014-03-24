package name.graf.emanuel.testfileeditor.ui;

import java.util.ArrayList;

public class TestFile
{
    private final String name;
    private final ArrayList<Test> tests;
    
    public TestFile(final String name) {
        super();
        this.tests = new ArrayList<Test>();
        this.name = name;
    }
    
    @Override
	public String toString() {
        return this.name;
    }
    
    public Test[] getTests() {
		return this.tests.toArray(new Test[0]);
    }
    
    public void addTest(final Test test) {
        this.tests.add(test);
    }
    
    public void clear() {
        this.tests.clear();
    }
    
    @Override
	public int hashCode() {
        return this.name.hashCode();
    }
    
    @Override
	public boolean equals(final Object obj) {
        return this.hashCode() == obj.hashCode();
    }
}
