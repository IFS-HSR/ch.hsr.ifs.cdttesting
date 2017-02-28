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
 *         An extended QuickfixTest that allows to set individual preferences for
 *         each test.
 *
 *         Usage:
 *
 *         Without evaluation -> ( key | value ) can be chained using commas.
 *
 *           //!TestFoo
 *           //@.config
 *           setPreferences=(com.cevelop.intwidthfixator.intMappingLength|com.cevelop.intwidthfixator.size.16)
 *           //@main.cpp
 *           int foo {42};
 *
 *
 *
 *		   With evaluation -> ( field-name key | field-name value) can be chained using commas.
 *
 *           //!TestBar
 *           //@.config
 *           setPreferencesEval=(P_CHAR_MAPPING_TO_FIXED|V_SIZE_16),(P_CHAR_PLATFORM_SIGNED_UNSIGNED|V_CHAR_PLATFORM_UNSIGNED)
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
		final String setPreference = properties.getProperty("setPreferences");
		final String setPreferenceEval = properties.getProperty("setPreferencesEval");
		if (setPreference != null && !setPreference.isEmpty()) {
			if (preferenceStore == null) {
				preferenceStore = initPrefs();
			}
			final String[] splitPreferences = setPreference.split(",");
			final Map<String, String> preferencesMap = new HashMap<>();
			splitAndAdd(preferencesMap, splitPreferences);
			backupPreferences(prefBackup, preferencesMap);
			setPreferences(preferencesMap);
		}
		if (setPreferenceEval != null && !setPreferenceEval.isEmpty()) {

			if (preferenceStore == null) {
				preferenceStore = initPrefs();
			}
			final String[] splitPreferences = setPreferenceEval.split(",");
			final Map<String, String> preferencesMap = new HashMap<>();
			splitAndAdd(preferencesMap, splitPreferences);
			final Map<String, String> evaluatedMap = evaluatePreferences(preferencesMap);
			backupPreferences(prefEvalBackup, evaluatedMap);
			setPreferences(evaluatedMap);
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
		setPreferences(prefBackup);
		setPreferences(prefEvalBackup);
		super.tearDown();
		cleanupProject();
	}

	private void backupPreferences(final Map<String, String> backupMap, final Map<String, String> preferencesMap) {
		for (final String key : preferencesMap.keySet()) {
			final String value = preferenceStore.getString(key);
			backupMap.put(key, value);
		}
	}

	private void setPreferences(final Map<String, String> preferencesMap) {
		for (final String key : preferencesMap.keySet()) {
			preferenceStore.setValue(key, preferencesMap.get(key));
		}
	}

	private void splitAndAdd(final Map<String, String> preferencesMap, final String[] splitPreferences) {
		for (final String s : splitPreferences) {
			final String[] pair = s.substring(1, s.length() - 1).split("\\|");
			preferencesMap.put(pair[0], pair[1]);
		}
	}

	private Map<String, String> evaluatePreferences(final Map<String, String> preferencesMap) {
		final Map<String, String> evaluatedMap = new HashMap<>();
		try {
			for (final String key : preferencesMap.keySet()) {
				final Field evaluatedKey = getPreferenceConstants().getDeclaredField(key);
				final Field evaluatedValue = getPreferenceConstants().getDeclaredField(preferencesMap.get(key));
				evaluatedMap.put((String) evaluatedKey.get(null), (String) evaluatedValue.get(null));
			}
		} catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return evaluatedMap;
	}
}
