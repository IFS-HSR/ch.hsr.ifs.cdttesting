package name.graf.emanuel.testfileeditor.ui;

import name.graf.emanuel.testfileeditor.*;
import org.eclipse.jface.preference.*;
import org.eclipse.ui.*;

public class TestFilePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage
{
    public TestFilePreferences() {
        super(1);
        this.setPreferenceStore(Activator.getDefault().getPreferenceStore());
        this.setDescription("A demonstration of a preference page implementation");
    }
    
    public void createFieldEditors() {
        this.addField((FieldEditor)new StringFieldEditor("nameStart", "Test Name Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("langStart", "Language Definition Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("expecStart", "Expected Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("comStart", "Comment Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("comEnd", "Comment End Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("className", "Class Name Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("fileName", "File Name Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("selectionStart", "Selection Start Tag:", this.getFieldEditorParent()));
        this.addField((FieldEditor)new StringFieldEditor("selectionEnd", "Selection End Tag:", this.getFieldEditorParent()));
    }
    
    public void init(final IWorkbench workbench) {
    }
}
