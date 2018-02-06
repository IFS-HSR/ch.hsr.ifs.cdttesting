package ch.hsr.ifs.pasta.plugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import ch.hsr.ifs.pasta.PastaPlugin;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

   /*
    * (non-Javadoc)
    * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
    * initializeDefaultPreferences()
    */
   @Override
   public void initializeDefaultPreferences() {
      final IPreferenceStore store = PastaPlugin.getDefault().getPreferenceStore();
      store.setDefault(PreferenceConstants.P_HOW_TO_SELECT, PreferenceConstants.P_SELECT_BY_MOUSE_OVER);
   }

}
