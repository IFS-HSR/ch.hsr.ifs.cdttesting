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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.junit.After;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;


public class CheckNoUnresolvedInclusionsTest extends CDTTestingTest implements ILogListener {

   IStatus loggedStatus;
   String  loggingPlugin;

   @Override
   public void setUp() throws Exception {
      Plugin plugin = CCorePlugin.getDefault();
      if (plugin != null) {
         plugin.getLog().addLogListener(this);
      }
      super.setUp();
   }

   @Test
   public void runTest() throws Throwable {
      // logging by indexer happens in super.setUp() call
      assertNotNull(loggedStatus);
      assertNull(loggedStatus.getException());
      assertEquals(CCorePlugin.PLUGIN_ID, loggingPlugin);
      assertEquals(IStatus.INFO, loggedStatus.getSeverity());
      assertTrue(loggedStatus.getMessage().contains("1 declarations; 0 references; 0 unresolved inclusions; 0 syntax errors; 0 unresolved names"));
      assertTrue(loggedStatus.getMessage().startsWith("Indexed '" + currentProject.getName() + "' (1 sources, 0 headers)"));
   }

   @Override
   public void logging(IStatus status, String plugin) {
      loggedStatus = status;
      loggingPlugin = plugin;
   }

   @Override
   @After
   public void tearDown() throws Exception {
      Plugin plugin = CCorePlugin.getDefault();
      if (plugin != null) {
         plugin.getLog().removeLogListener(this);
      }
      super.tearDown();
   }
}
