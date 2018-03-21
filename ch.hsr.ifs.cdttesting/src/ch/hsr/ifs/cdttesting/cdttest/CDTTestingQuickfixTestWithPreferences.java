package ch.hsr.ifs.cdttesting.cdttest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;

import ch.hsr.ifs.cdttesting.cdttest.base.ITestPreferencesMixin;


/**
 * @author tstauber
 *
 *         An extended QuickfixTest that allows to set individual preferences for
 *         each test.
 **/
public abstract class CDTTestingQuickfixTestWithPreferences extends CDTTestingQuickfixTest implements ITestPreferencesMixin {

   private final Map<String, String> prefBackup     = new HashMap<>();
   private final Map<String, String> prefEvalBackup = new HashMap<>();
   protected IPreferenceStore        preferenceStore;

   @Override
   protected void configureTest(final Properties properties) {
      final String setPreference = properties.getProperty(CDTTestingConfigConstants.SET_PREFERENCES);
      final String setPreferenceEval = properties.getProperty(CDTTestingConfigConstants.SET_PREFERENCES_EVAL);
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

   @Override
   public void tearDown() throws Exception {
      setPreferences(prefBackup);
      setPreferences(prefEvalBackup);
      super.tearDown();
      cleanupProjects();
   }

   @Override
   public void backupPreferences(final Map<String, String> backupMap, final Map<String, String> preferencesMap) {
      for (final String key : preferencesMap.keySet()) {
         final String value = preferenceStore.getString(key);
         backupMap.put(key, value);
      }
   }

   @Override
   public void setPreferences(final Map<String, String> preferencesMap) {
      for (final String key : preferencesMap.keySet()) {
         preferenceStore.setValue(key, preferencesMap.get(key));
      }
   }

}
