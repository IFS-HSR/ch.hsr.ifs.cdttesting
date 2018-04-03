/*******************************************************************************
 * Copyright (c) 2011 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.rts.junit4;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import ch.hsr.ifs.cdttesting.cdttest.base.SourceFileBaseTest;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest;


/**
 * 
 * This class is an adaptation of the Parameterized class from JUnit4.
 * 
 */
public class RtsTestSuite extends Suite {

   private class RTSTestRunner extends BlockJUnit4ClassRunner {

      private final RTSTest test;

      RTSTestRunner(Class<?> type, RTSTest test) throws InitializationError {
         super(type);
         this.test = test;
      }

      @Override
      public Object createTest() throws Exception {
         SourceFileBaseTest testInstance = (SourceFileBaseTest) getTestClass().getOnlyConstructor().newInstance();
         testInstance.setName(test.getName());
         testInstance.setLanguage(test.getLanguage());
         testInstance.initTestSourceFiles(test.getTestSourceFiles());
         return testInstance;
      }

      @Override
      protected String getName() {
         return test.getName();
      }

      @Override
      protected String testName(final FrameworkMethod method) {
         return String.format("%s[%s]", method.getName(), getName());
      }

      @Override
      protected void validateConstructor(List<Throwable> errors) {
         validateOnlyOneConstructor(errors);
      }

      @Override
      protected Statement classBlock(RunNotifier notifier) {
         return childrenInvoker(notifier);
      }
   }

   private final ArrayList<Runner> runners = new ArrayList<Runner>();

   /**
    * Only called reflectively. Do not use programmatically.
    */
   public RtsTestSuite(Class<?> clazz) throws Throwable {
      super(clazz, Collections.<Runner>emptyList());
      for (RTSTest testCase : getParametersList(getTestClass()))
         runners.add(new RTSTestRunner(getTestClass().getJavaClass(), testCase));
   }

   @Override
   protected List<Runner> getChildren() {
      return runners;
   }

   @SuppressWarnings("unchecked")
   private ArrayList<RTSTest> getParametersList(TestClass clazz) throws Throwable {
      return (ArrayList<RTSTest>) getParametersMethod(clazz).invokeExplosively(null, clazz.getJavaClass());
   }

   private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
      List<FrameworkMethod> methods = testClass.getAnnotatedMethods(RTSTestCases.class);

      for (FrameworkMethod each : methods) {
         int modifiers = each.getMethod().getModifiers();
         if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) return each;
      }
      throw new Exception("No public static parameters method on class " + testClass.getName());
   }
}
