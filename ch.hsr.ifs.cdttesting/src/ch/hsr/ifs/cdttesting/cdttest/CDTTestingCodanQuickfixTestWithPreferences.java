package ch.hsr.ifs.cdttesting.cdttest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;

//@formatter:off
/**
 * @author tstauber
 *
 *         An extended QuickfixTest that allows to set individual settings for
 *         each test.
 *
 *         Usage:
 *
 *         Without evaluation -> ( key | value ) can be chained using commas.
 *
 *           //!TestFoo
 *           //@.config
 *           setSettings=(com.cevelop.intwidthfixator.intMappingLength|com.cevelop.intwidthfixator.size.16)
 *           //@main.cpp
 *           int foo {42};
 *
 *
 *
 *		   With evaluation -> ( field-name key | field-name value) can be chained using commas.
 *
 *           //!TestBar
 *           //@.config
 *           setSettingsEval=(P_CHAR_MAPPING_TO_FIXED|V_SIZE_16),(P_CHAR_PLATFORM_SIGNED_UNSIGNED|V_CHAR_PLATFORM_UNSIGNED)
 *           //@main.cpp
 *           char foo {42};
 *
 */
//@formatter:on
public abstract class CDTTestingCodanQuickfixTestWithPreferences extends CDTTestingCodanQuickfixTest {
	private final Map<String, String> prefBackup = new HashMap<>();
	private final Map<String, String> prefEvalBackup = new HashMap<>();
	protected static IPreferenceStore preferenceStore;

	@Override
	protected void configureTest(final Properties properties) {
		final String setSettings = properties.getProperty("setSettings");
		final String setSettingsEval = properties.getProperty("setSettingsEval");
		if (setSettings != null && !setSettings.isEmpty()) {
			if (preferenceStore == null) {
				preferenceStore = initPrefs();
			}
			final String[] splitSettings = setSettings.split(",");
			final Map<String, String> settingsMap = new HashMap<>();
			splitAndAdd(settingsMap, splitSettings);
			backupSettings(prefBackup, settingsMap);
			setSettings(settingsMap);
		}
		if (setSettingsEval != null && !setSettingsEval.isEmpty()) {

			if (preferenceStore == null) {
				preferenceStore = initPrefs();
			}
			final String[] splitSettings = setSettingsEval.split(",");
			final Map<String, String> settingsMap = new HashMap<>();
			splitAndAdd(settingsMap, splitSettings);
			final Map<String, String> evaluatedMap = interpretSettings(settingsMap);
			backupSettings(prefEvalBackup, evaluatedMap);
			setSettings(evaluatedMap);
		}
		super.configureTest(properties);
	}

	/**
	 * @author tstauber
	 *
	 * @return The {@code IPreferenceStore} containing the preferences that
	 *         shall be altered while testing.
	 *
	 */
	public abstract IPreferenceStore initPrefs();

	/**
	 * @author tstauber
	 *
	 * @return The {@code Class} containing the static fields that contain the
	 *         {@code String}s representing the id's for the preferences.
	 *
	 */
	public abstract Class getPreferenceConstants();

	@Override
	public void tearDown() throws Exception {
		setSettings(prefBackup);
		setSettings(prefEvalBackup);
		super.tearDown();
		cleanupProject();
	}

	private void backupSettings(final Map<String, String> backupMap, final Map<String, String> settingsMap) {
		for (final String key : settingsMap.keySet()) {
			final String value = preferenceStore.getString(key);
			backupMap.put(key, value);
		}
	}

	private void setSettings(final Map<String, String> settingsMap) {
		for (final String key : settingsMap.keySet()) {
			preferenceStore.setValue(key, settingsMap.get(key));
		}
	}

	private void splitAndAdd(final Map<String, String> settingsMap, final String[] splitSettings) {
		for (final String s : splitSettings) {
			final String[] pair = s.substring(1, s.length() - 1).split("\\|");
			settingsMap.put(pair[0], pair[1]);
		}
	}

	private Map<String, String> interpretSettings(final Map<String, String> settingsMap) {
		final Map<String, String> evaluatedMap = new HashMap<>();
		try {
			for (final String key : settingsMap.keySet()) {
				final Field evaluatedKey = getPreferenceConstants().getDeclaredField(key);
				final Field evaluatedValue = getPreferenceConstants().getDeclaredField(settingsMap.get(key));
				evaluatedMap.put((String) evaluatedKey.get(null), (String) evaluatedValue.get(null));
			}
		} catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return evaluatedMap;
	}
}
