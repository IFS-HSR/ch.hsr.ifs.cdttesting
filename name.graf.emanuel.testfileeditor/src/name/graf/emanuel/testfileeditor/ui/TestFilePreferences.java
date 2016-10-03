package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import name.graf.emanuel.testfileeditor.Activator;

public class TestFilePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public TestFilePreferences() {
		super(GRID);
		setDescription("Define the tags used to specify the different test-file sections:");
	}

	public void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceConstants.P_TEST_NAME_START, "Test Name Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_LANG_START, "Language Definition Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_EXPECTED_START, "Expected Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_COMMENT_START, "Comment Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_COMMENT_END, "Comment End Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_CLASS_NAME, "Class Name Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_FILE_NAME, "File Name Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_SELECTION_START, "Selection Start Tag:",
				this.getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_SELECTION_END, "Selection End Tag:",
				this.getFieldEditorParent()));
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

}
