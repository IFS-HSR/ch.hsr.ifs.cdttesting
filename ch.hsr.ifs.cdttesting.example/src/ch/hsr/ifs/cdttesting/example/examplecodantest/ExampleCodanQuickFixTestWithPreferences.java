package ch.hsr.ifs.cdttesting.example.examplecodantest;

import static org.junit.Assert.assertEquals;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanQuickfixTestWithPreferences;
import ch.hsr.ifs.cdttesting.example.examplecodantest.MyCodanChecker.MyProblemId;
import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


public class ExampleCodanQuickFixTestWithPreferences extends CDTTestingCodanQuickfixTestWithPreferences {

   @Override
   protected IProblemId getProblemId() {
      return MyProblemId.EXAMPLE_ID;
   }

   @Test
   public void runTest() throws Throwable {
      runQuickFix(new MyQuickFix());
      assertEquals(getExpectedSource(), getCurrentSource());
   }

   @Override
   public IPreferenceStore initPrefs() {
      return TestingPlugin.getDefault().getPreferenceStore();
   }

   @Override
   @SuppressWarnings("rawtypes")
   public Class getPreferenceConstants() {
      return PrefConstants.class;
   }

   // Can be anywhere in the project.
   class PrefConstants {

      public static final String P_PREF_FOO = TestingPlugin.PLUGIN_ID + ".preference.foo";
      public static final String P_PREF_BAR = TestingPlugin.PLUGIN_ID + ".preference.bar";
      public static final String P_PREF_BAZ = TestingPlugin.PLUGIN_ID + ".preference.baz";
   };
}
