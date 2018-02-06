package ch.hsr.ifs.pasta.plugin.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.hsr.ifs.pasta.PastaPlugin;


public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public PreferencePage() {
      super(GRID);
      setPreferenceStore(PastaPlugin.getDefault().getPreferenceStore());
      setDescription(
            "This preference page allows to define how the node in \"AST View\" shall be selected to actualize the current node in the \"Node View\".");
   }

   @Override
   public void createFieldEditors() {
      final String[][] labelAndValues = { { "By &hovering over the node", PreferenceConstants.P_SELECT_BY_MOUSE_OVER }, { "By &left click",
                                                                                                                          PreferenceConstants.P_SELECT_BY_LEFT_CLICK },
                                          { "By &right click", PreferenceConstants.P_SELECT_BY_RIGHT_CLICK } };

      addField(new RadioGroupFieldEditor(PreferenceConstants.P_HOW_TO_SELECT, "How shall the displayed node be selected?", 1, labelAndValues,
            getFieldEditorParent()));
   }

   @Override
   public void init(final IWorkbench workbench) {}

}
