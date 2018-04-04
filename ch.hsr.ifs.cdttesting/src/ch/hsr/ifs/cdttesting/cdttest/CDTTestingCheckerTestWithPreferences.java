package ch.hsr.ifs.cdttesting.cdttest;

import java.util.Properties;

import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.ITestPreferencesMixinHost;
import ch.hsr.ifs.cdttesting.cdttest.base.preferencemixin.TestPreferencesMixin;


/**
 * @author tstauber
 *
 *         An extended CheckerTest that allows to set individual preferences for
 *         each test.
 **/
public abstract class CDTTestingCheckerTestWithPreferences extends CDTTestingCheckerTest implements ITestPreferencesMixinHost {

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
