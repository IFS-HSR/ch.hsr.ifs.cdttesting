package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.core.runtime.preferences.*;
import name.graf.emanuel.testfileeditor.*;

public class PreferenceInitializer extends AbstractPreferenceInitializer
{
	@Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences preferences = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);

        preferences.put(PreferenceConstants.P_TEST_NAME_START, PreferenceConstants.D_TEST_NAME_START);
        preferences.put(PreferenceConstants.P_TEST_NAME_END, PreferenceConstants.D_TEST_NAME_END);
        preferences.put(PreferenceConstants.P_LANG_START, PreferenceConstants.D_LANG_START);
        preferences.put(PreferenceConstants.P_LANG_END, PreferenceConstants.D_LANG_END);
        preferences.put(PreferenceConstants.P_EXPECTED_START, PreferenceConstants.D_EXPECTED_START);
        preferences.put(PreferenceConstants.P_EXPECTED_END, PreferenceConstants.D_EXPECTED_END);
        preferences.put(PreferenceConstants.P_COMMENT_START, PreferenceConstants.D_COMMENT_START);
        preferences.put(PreferenceConstants.P_COMMENT_END, PreferenceConstants.D_COMMENT_END);
        preferences.put(PreferenceConstants.P_CLASS_NAME, PreferenceConstants.D_CLASS_NAME);
        preferences.put(PreferenceConstants.P_FILE_NAME, PreferenceConstants.D_FILE_NAME);
        preferences.put(PreferenceConstants.P_SELECTION_START, PreferenceConstants.D_SELECTION_START);
        preferences.put(PreferenceConstants.P_SELECTION_END, PreferenceConstants.D_SELECTION_END);
    }
}
