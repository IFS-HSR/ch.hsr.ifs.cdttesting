package ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingConfigConstants;
import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.ITestPreferencesMixin;


public class TestPreferencesMixin implements ITestPreferencesMixin {

   private final Map<String, String>   prefBackup     = new HashMap<>();
   private final Map<String, String>   prefEvalBackup = new HashMap<>();
   protected IPreferenceStore          preferenceStore;
   protected ITestPreferencesMixinHost host;

   public TestPreferencesMixin(ITestPreferencesMixinHost host) {
      this.host = host;
   }

   public ITestPreferencesMixinHost getHost() {
      return host;
   }

   @Override
   public void setupPreferences(Properties properties) {
      final String preference = properties.getProperty(CDTTestingConfigConstants.SET_PREFERENCES);
      final String preferenceEval = properties.getProperty(CDTTestingConfigConstants.SET_PREFERENCES_EVAL);

      if (preference != null && !preference.isEmpty()) {
         if (preferenceStore == null) {
            preferenceStore = getHost().initPrefs();
         }
         final String[] splitPreferences = preference.split(",");
         final Map<String, String> preferencesMap = new HashMap<>();
         splitAndAdd(preferencesMap, splitPreferences);
         backupPreferences(prefBackup, preferencesMap);
         setPreferences(preferencesMap);
      }
      if (preferenceEval != null && !preferenceEval.isEmpty()) {

         if (preferenceStore == null) {
            preferenceStore = getHost().initPrefs();
         }
         final String[] splitPreferences = preferenceEval.split(",");
         final Map<String, String> preferencesMap = new HashMap<>();
         splitAndAdd(preferencesMap, splitPreferences);
         final Map<String, String> evaluatedMap = evaluatePreferences(preferencesMap);
         backupPreferences(prefEvalBackup, evaluatedMap);
         setPreferences(evaluatedMap);
      }
   }

   @Override
   public void resetPreferences() {
      setPreferences(prefBackup);
      setPreferences(prefEvalBackup);
   }

   protected void backupPreferences(final Map<String, String> backupMap, final Map<String, String> preferencesMap) {
      for (final String key : preferencesMap.keySet()) {
         final String value = preferenceStore.getString(key);
         backupMap.put(key, value);
      }
   }

   protected void setPreferences(final Map<String, String> preferencesMap) {
      for (final String key : preferencesMap.keySet()) {
         preferenceStore.setValue(key, preferencesMap.get(key));
      }
   }

   protected void splitAndAdd(final Map<String, String> preferencesMap, final String[] splitPreferences) {
      for (final String s : splitPreferences) {
         final String[] pair = s.substring(1, s.length() - 1).split("\\|");
         preferencesMap.put(pair[0].trim(), pair[1].trim());
      }
   }

   protected Map<String, String> evaluatePreferences(final Map<String, String> preferencesMap) {
      final Map<String, String> evaluatedMap = new HashMap<>();
      final Class<?> prefConstants = getHost().getPreferenceConstants();
      try {
         for (final String key : preferencesMap.keySet()) {
            final Field evaluatedKey = prefConstants.getDeclaredField(key);
            final Field evaluatedValue = prefConstants.getDeclaredField(preferencesMap.get(key));
            evaluatedMap.put((String) evaluatedKey.get(null), (String) evaluatedValue.get(null));
         }
      } catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
         e.printStackTrace();
      }

      return evaluatedMap;
   }

}
