package ch.hsr.ifs.cdttesting.cdttest.base;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Usage:
 *
 * Without evaluation -> ( key | value ) can be chained using commas.
 *
 * //!TestFoo
 * //@.config
 * setPreferences=(com.cevelop.intwidthfixator.intMappingLength|com.cevelop.intwidthfixator.size.16)
 * //@main.cpp
 * int foo {42};
 *
 *
 *
 * With evaluation -> ( field-name key | field-name value) can be chained using commas.
 *
 * //!TestBar
 * //@.config
 * setPreferencesEval=(P_CHAR_MAPPING_TO_FIXED|V_SIZE_16),(P_CHAR_PLATFORM_SIGNED_UNSIGNED|V_CHAR_PLATFORM_UNSIGNED)
 * //@main.cpp
 * char foo {42};
 * 
 * @author tstauber
 *
 */
public interface ITestPreferencesMixin {

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
   public abstract Class<?> getPreferenceConstants();

   abstract void backupPreferences(final Map<String, String> backupMap, final Map<String, String> preferencesMap);

   abstract void setPreferences(final Map<String, String> preferencesMap);

   default void splitAndAdd(final Map<String, String> preferencesMap, final String[] splitPreferences) {
      for (final String s : splitPreferences) {
         final String[] pair = s.substring(1, s.length() - 1).split("\\|");
         preferencesMap.put(pair[0], pair[1]);
      }
   }
   
   default Map<String, String> evaluatePreferences(final Map<String, String> preferencesMap) {
      final Map<String, String> evaluatedMap = new HashMap<>();
      final Class<?> prefConstants = getPreferenceConstants();
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
