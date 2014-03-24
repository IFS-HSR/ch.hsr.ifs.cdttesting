package name.graf.emanuel.testfileeditor.ui;

import org.eclipse.core.runtime.preferences.*;
import name.graf.emanuel.testfileeditor.*;
import org.eclipse.jface.preference.*;

public class PreferenceInitializer extends AbstractPreferenceInitializer
{
    public void initializeDefaultPreferences() {
        final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault("nameStart", "//!");
        store.setDefault("nameEnd", "\\n");
        store.setDefault("langStart", "//%");
        store.setDefault("langEnd", "\\n");
        store.setDefault("expecStart", "//=");
        store.setDefault("expecEnd", "\\n");
        store.setDefault("comStart", "/*");
        store.setDefault("comEnd", "*/");
        store.setDefault("className", "//#");
        store.setDefault("fileName", "//@");
        store.setDefault("selectionStart", "//$");
        store.setDefault("selectionEnd", "$//");
    }
}
