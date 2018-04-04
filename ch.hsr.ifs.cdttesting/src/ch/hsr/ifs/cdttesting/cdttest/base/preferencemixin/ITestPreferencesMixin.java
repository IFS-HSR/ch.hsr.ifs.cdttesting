package ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin;

import java.util.Properties;

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

   abstract void setupPreferences(Properties properties);

   abstract void resetPreferences();

   abstract ITestPreferencesMixinHost getHost();

}
