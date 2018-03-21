package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMarkerResolution;
import org.junit.Test;

import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import ch.hsr.ifs.cdttesting.cdttest.CDTTestingQuickfixTestWithPreferences;
import ch.hsr.ifs.cdttesting.example.examplecodantest.MyCodanChecker.MyProblemId;


public class ExampleCodanQuickFixTestWithPreferences extends CDTTestingQuickfixTestWithPreferences {

   @Override
   protected IProblemId getProblemId() {
      return MyProblemId.EXAMPLE_ID;
   }

   @Test
   public void runTest() throws Throwable {
      runQuickfixAndAssertAllEqual();
   }

   @Override
   protected IMarkerResolution createMarkerResolution() {
      return new MyQuickFix();
   }

   @Override
   public IPreferenceStore initPrefs() {
      return TestingPlugin.getDefault().getPreferenceStore();
   }

   @Override
   public Class<?> getPreferenceConstants() {
      return PrefConstants.class;
   }

   // Can be anywhere in the project.
   class PrefConstants {

      public static final String P_PREF_FOO = TestingPlugin.PLUGIN_ID + ".preference.foo";
      public static final String P_PREF_BAR = TestingPlugin.PLUGIN_ID + ".preference.bar";
      public static final String P_PREF_BAZ = TestingPlugin.PLUGIN_ID + ".preference.baz";
   };
}
