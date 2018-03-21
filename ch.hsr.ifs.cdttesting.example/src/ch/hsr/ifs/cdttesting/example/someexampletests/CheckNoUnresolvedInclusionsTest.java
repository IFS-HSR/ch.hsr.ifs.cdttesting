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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.junit.After;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;


public class CheckNoUnresolvedInclusionsTest extends CDTTestingUITest implements ILogListener {

   private Map<String, IStatus> logs = new HashMap<>();

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
      assertFalse(logs.isEmpty());
      logs.values().stream().forEach(status -> {
         assertNull(status.getException());
         assertEquals(IStatus.INFO, status.getSeverity());
      });
      logs.keySet().stream().forEach(id -> assertEquals(CCorePlugin.PLUGIN_ID, id));
      assertTrue(logs.values().stream().anyMatch(status -> status.getMessage().contains(
            "1 declarations; 0 references; 0 unresolved inclusions; 0 syntax errors; 0 unresolved names")));
      /* As the projects are created an indexed in parallel, the log is written with race condition */
   }

   @Override
   public void logging(IStatus status, String plugin) {
      logs.put(plugin, status);
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
