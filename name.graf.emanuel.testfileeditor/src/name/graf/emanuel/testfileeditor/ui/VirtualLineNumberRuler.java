package name.graf.emanuel.testfileeditor.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;

import name.graf.emanuel.testfileeditor.model.TestFile;
import name.graf.emanuel.testfileeditor.model.node.Expected;
import name.graf.emanuel.testfileeditor.model.node.File;
import name.graf.emanuel.testfileeditor.model.node.Node;
import name.graf.emanuel.testfileeditor.model.node.Test;

@SuppressWarnings("restriction")
public class VirtualLineNumberRuler extends LineNumberRulerColumn implements IContributedRulerColumn, Observer {

    private Editor textEditor;
    private RulerColumnDescriptor columnDescriptor;
    private ITextViewer textWidget;
    private Map<Integer, Integer> modelLineToRulerLineMap = new HashMap<>();
    private int maxDigits = 0;

    @Override
    protected int computeNumberOfDigits() {
        return maxDigits + 1;
    }

    @Override
    public void columnCreated() {
    }

    @Override
    public void columnRemoved() {
    }

    @Override
    public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
        textWidget = parentRuler.getTextViewer();
        TestFile file = textEditor.getAdapter(TestFile.class);
        file.addObserver(this);
        update(file, null);

        RGB foreground = getColor(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR);
        RGB background = getColor(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);

        setForeground(EditorsPlugin.getDefault().getSharedTextColors().getColor(foreground));
        setBackground(EditorsPlugin.getDefault().getSharedTextColors().getColor(background));

        return super.createControl(parentRuler, parentControl);
    }

    @Override
    protected String createDisplayString(int line) {
        line = JFaceTextUtil.widgetLine2ModelLine(textWidget, line);
        return modelLineToRulerLineMap.containsKey(line) ? modelLineToRulerLineMap.get(line).toString() : "";
    }

    private RGB getColor(String key) {
        IPreferenceStore preferenceStore = EditorsUI.getPreferenceStore();
        if (preferenceStore.contains(key)) {
            return PreferenceConverter.getColor(preferenceStore, key);
        }

        return PreferenceConverter.getDefaultColor(preferenceStore, key);
    }

    @Override
    public RulerColumnDescriptor getDescriptor() {
        return columnDescriptor;
    }

    @Override
    public ITextEditor getEditor() {
        return textEditor;
    }

    @Override
    public void setDescriptor(RulerColumnDescriptor descriptor) {
        columnDescriptor = descriptor;
    }

    @Override
    public void setEditor(ITextEditor editor) {
        textEditor = (Editor) editor;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TestFile) {
            List<Integer> startLineNumbers = new ArrayList<>();
            List<Integer> endLineNumbers = new ArrayList<>();

            TestFile file = (TestFile) o;
            
            IDocument document = getEditor().getDocumentProvider().getDocument(getEditor().getEditorInput());
            Test[] tests = file.getTests();

            try {
                for (Test test : tests) {
                    Node[] files = test.getChildren();
                    Position nodePosition = test.getPosition();
                    int realLine = document.getLineOfOffset(nodePosition.offset);
                    endLineNumbers.add(realLine + 1);
                    for (Node testNode : files) {
                        nodePosition = testNode.getPosition();
                        realLine = document.getLineOfOffset(nodePosition.offset) + 1;

                        if (testNode instanceof File) {
                            String name = testNode.toString();
                            if (name.endsWith(".cpp") || name.endsWith(".h") || name.endsWith(".hpp")) {
                                startLineNumbers.add(realLine);
                            } else {
                                endLineNumbers.add(realLine);
                            }
                        } else if (testNode instanceof Expected) {
                            startLineNumbers.add(realLine);
                        } else {
                            endLineNumbers.add(realLine);
                        }
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            modelLineToRulerLineMap.clear();
            int largestNumber = 0;
            int lines = document.getNumberOfLines() + 1;
            boolean inFile = false;
            int rulerLine = 1;
            for (int line = 1; line < lines; ++line) {
                if (startLineNumbers.contains(line)) {
                    inFile = true;
                    rulerLine = 1;
                    continue;
                } else if (endLineNumbers.contains(line)) {
                    inFile = false;
                } else if (inFile) {
                    if (rulerLine > largestNumber) {
                        largestNumber = rulerLine;
                    }
                    modelLineToRulerLineMap.put(line - 1, rulerLine++);
                }
            }

            maxDigits = Integer.toString(largestNumber).length();
            redraw();
        }
    }

}
