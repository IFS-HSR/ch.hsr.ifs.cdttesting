/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;


public class WithConfigFileTest extends CDTTestingTest {

   private String  stringProperty;
   private int     intProperty;
   private boolean boolProperty;

   @Test
   public void runTest() throws Throwable {
      assertEquals("someValue", stringProperty);
      assertEquals(7, intProperty);
      assertTrue(boolProperty);
   }

   @Override
   protected void configureTest(Properties properties) {
      stringProperty = properties.getProperty("someKey");
      intProperty = Integer.parseInt(properties.getProperty("someOtherKey", "0")); // passing 0 as default if property
      // isn't existing, otherwise you
      // will get a NPE.
      boolProperty = Boolean.parseBoolean(properties.getProperty("yetAnotherKey", "false")); // again, passing false
      // as default.
   }
}
