package ch.hsr.ifs.cdttesting.cdttest;

import java.util.Properties;

import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.ITestPreferencesMixinHost;
import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.TestPreferencesMixin;


/**
 * An extended {@link CDTTestingQuickfixTest} that allows to set individual preferences for each test.
 * <p>
 * Usage:
 * <pre>
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
 * </pre>
 * @author tstauber
 *
 */
public abstract class CDTTestingQuickfixTestWithPreferences extends CDTTestingQuickfixTest implements ITestPreferencesMixinHost {

   private TestPreferencesMixin testPreferenceMixin = new TestPreferencesMixin(this);

   @Override
   protected void configureTest(final Properties properties) {
      super.configureTest(properties);
      testPreferenceMixin.setupPreferences(properties);
   }

   @Override
   public void tearDown() throws Exception {
      testPreferenceMixin.resetPreferences();
      super.tearDown();
   }

}
