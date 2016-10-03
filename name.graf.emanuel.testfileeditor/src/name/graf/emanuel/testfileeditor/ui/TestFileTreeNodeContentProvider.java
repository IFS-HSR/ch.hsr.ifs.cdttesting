package name.graf.emanuel.testfileeditor.ui;

import name.graf.emanuel.testfileeditor.Activator;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class TestFileTreeNodeContentProvider extends TreeNodeContentProvider
{
    protected IPositionUpdater fPositionUpdater;
    private final IDocumentProvider provider;
    private TestFile file;
    private final IEditorInput input;
    private final String testTag;
    private final String langTag;
    private final String expTag;
    private final String fileTag;
    private final String classNameTag;
    private final String selStartTag;
    private final String selEndTag;
    private ParseState state;
    private static /* synthetic */ int[] $SWITCH_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState;
    
    public TestFileTreeNodeContentProvider(final IDocumentProvider provider, final IEditorInput input) {
        super();
        this.fPositionUpdater = new DefaultPositionUpdater(Activator.TEST_FILE_PARTITIONING);
        this.state = ParseState.UNDEF;
        this.provider = provider;
        this.input = input;
        this.file = new TestFile(input.getName());
        IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        this.testTag = pref.get(PreferenceConstants.P_TEST_NAME_START, PreferenceConstants.D_TEST_NAME_START);
        this.langTag = pref.get(PreferenceConstants.P_LANG_START, PreferenceConstants.D_LANG_START);
        this.expTag = pref.get(PreferenceConstants.P_EXPECTED_START, PreferenceConstants.D_EXPECTED_START);
        this.fileTag = pref.get(PreferenceConstants.P_FILE_NAME, PreferenceConstants.D_FILE_NAME);
        this.classNameTag = pref.get(PreferenceConstants.P_CLASS_NAME, PreferenceConstants.D_CLASS_NAME);
        this.selStartTag = pref.get(PreferenceConstants.P_SELECTION_START, PreferenceConstants.D_SELECTION_START);
        this.selEndTag = pref.get(PreferenceConstants.P_SELECTION_END, PreferenceConstants.D_SELECTION_END);
    }
    
    protected void parse(final IDocument document) {
        final int lines = document.getNumberOfLines();
        Test actTest = null;
        FileDefNode actFile = null;
        int selStart = 0;
        for (int line = 0; line < lines; ++line) {
            try {
                final int offset = document.getLineOffset(line);
                final int length = document.getLineLength(line);
                final String text = document.get(offset, length);
                if (text.startsWith(this.testTag)) {
                    final Position p = new Position(offset, length);
                    document.addPosition(Activator.TEST_FILE_PARTITIONING, p);
                    final Test testNode = actTest = new Test(text.substring(this.testTag.length()).trim(), p, this.file);
                    this.file.addTest(testNode);
                    this.state = ParseState.IN_TEST;
                }
                else if (text.startsWith(this.langTag)) {
                    if (actTest != null) {
                        final Position p = new Position(offset, length);
                        document.addPosition(Activator.TEST_FILE_PARTITIONING, p);
                        final LanguageDef lang = new LanguageDef(text.substring(this.langTag.length()).trim(), p, actTest);
                        actTest.setLang(lang);
                    }
                }
                else if (text.startsWith(this.expTag)) {
                    if (this.state == ParseState.IN_SELECTION) {
                        this.createSelection(document, actFile, selStart, offset);
                    }
                    switch ($SWITCH_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState()[this.state.ordinal()]) {
                        case 1: {
                            if (actTest != null) {
                                final Position p = new Position(offset, length);
                                document.addPosition(Activator.TEST_FILE_PARTITIONING, p);
                                final ExpectedNode exp = new ExpectedNode(actTest, text.substring(this.expTag.length()).trim(), p);
                                actTest.setExpected(exp);
                                break;
                            }
                            break;
                        }
                        case 2: {
                            if (actFile != null) {
                                final Position p = new Position(offset, length);
                                document.addPosition(Activator.TEST_FILE_PARTITIONING, p);
                                final ExpectedNode exp = new ExpectedNode(actTest, text.substring(this.expTag.length()).trim(), p);
                                actFile.setExpected(exp);
                                break;
                            }
                            break;
                        }
                    }
                }
                else if (text.startsWith(this.fileTag)) {
                    if (actTest != null) {
                        if (this.state == ParseState.IN_SELECTION) {
                            this.createSelection(document, actFile, selStart, offset);
                        }
                        final Position p = new Position(offset, length);
                        document.addPosition(Activator.TEST_FILE_PARTITIONING, p);
                        final FileDefNode fileDef = new FileDefNode(text.substring(this.fileTag.length()).trim(), p, actTest);
                        actTest.addFile(fileDef);
                        this.state = ParseState.IN_FILE;
                        actFile = fileDef;
                    }
                }
                else if (text.startsWith(this.classNameTag)) {
                    if (this.state == ParseState.IN_TEST) {
                        final Position p = new Position(offset, length);
                        document.addPosition(p);
                        final ClassNameNode className = new ClassNameNode(text.substring(this.classNameTag.length()).trim(), p, actTest);
                        actTest.setClassname(className);
                    }
                }
                else if (text.contains(this.selStartTag) && this.state == ParseState.IN_FILE) {
                    this.state = ParseState.IN_SELECTION;
                    selStart = offset + text.indexOf(this.selStartTag);
                }
                if (text.contains(this.selEndTag) && this.state == ParseState.IN_SELECTION) {
                    final int selEnd = offset + text.indexOf(this.selEndTag);
                    this.createSelection(document, actFile, selStart, selEnd);
                }
            }
            catch (BadPositionCategoryException ex) {}
            catch (BadLocationException ex2) {}
        }
    }
    
    private void createSelection(final IDocument document, final FileDefNode actFile, final int selStart, final int selEnd) throws BadLocationException {
        final Position p = new Position(selStart, selEnd - selStart);
        document.addPosition(p);
        actFile.setSelection(new SelectionNode(p, actFile));
        this.state = ParseState.IN_FILE;
    }
    
    @Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        if (oldInput != null) {
            final IDocument document = this.provider.getDocument(oldInput);
            if (document != null) {
                try {
                    document.removePositionCategory(Activator.TEST_FILE_PARTITIONING);
                }
                catch (BadPositionCategoryException ex) {}
                document.removePositionUpdater(this.fPositionUpdater);
            }
        }
        this.file.clear();
        if (newInput != null) {
            final IDocument document = this.provider.getDocument(newInput);
            if (document != null) {
                document.addPositionCategory(Activator.TEST_FILE_PARTITIONING);
                document.addPositionUpdater(this.fPositionUpdater);
                this.parse(document);
            }
        }
    }
    
    @Override
	public void dispose() {
        if (this.file != null) {
            this.file.clear();
            this.file = null;
        }
    }
    
    public boolean isDeleted(final Object element) {
        return false;
    }
    
    @Override
	public Object[] getElements(final Object element) {
		return new Object[] { this.file };
    }
    
    @Override
	public boolean hasChildren(final Object element) {
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode)element;
            return node.hasChildren();
        }
        final boolean ret = element == this.file || element == this.input;
        return ret;
    }
    
    @Override
	public Object getParent(final Object element) {
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode)element;
            return node.getParent();
        }
        if (element == this.file) {
            return this.input;
        }
        return null;
    }
    
    @Override
	public Object[] getChildren(final Object element) {
        if (element == this.file) {
            final Test[] tests = this.file.getTests();
            return tests;
        }
        if (element instanceof ITestFileNode) {
            final ITestFileNode node = (ITestFileNode)element;
            return node.getChildren();
        }
        return new Object[0];
    }
    
    static /* synthetic */ int[] $SWITCH_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState() {
        final int[] $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState = TestFileTreeNodeContentProvider.$SWITCH_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState;
        if ($switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState != null) {
            return $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState;
        }
        final int[] $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2 = new int[ParseState.values().length];
        try {
            $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2[ParseState.IN_FILE.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {}
        try {
            $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2[ParseState.IN_SELECTION.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError2) {}
        try {
            $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2[ParseState.IN_TEST.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError3) {}
        try {
            $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2[ParseState.UNDEF.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError4) {}
        return TestFileTreeNodeContentProvider.$SWITCH_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState = $switch_TABLE$name$graf$emanuel$testfileeditor$ui$TestFileTreeNodeContentProvider$ParseState2;
    }
    
    private enum ParseState
    {
        IN_TEST("IN_TEST", 0), 
        IN_FILE("IN_FILE", 1), 
        IN_SELECTION("IN_SELECTION", 2), 
        UNDEF("UNDEF", 3);

		private ParseState(String name, int ordinal) {

		}
    }
}
