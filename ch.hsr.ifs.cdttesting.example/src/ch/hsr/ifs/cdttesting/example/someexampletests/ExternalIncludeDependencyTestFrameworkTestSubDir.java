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

import java.io.File;

import org.eclipse.cdt.core.model.IIncludeReference;
import org.junit.Before;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;


public class ExternalIncludeDependencyTestFrameworkTestSubDir extends CDTTestingTest {

   @Override
   @Before
   public void setUp() throws Exception {
      addIncludeDirPath("externalFrameworkTestSubDir");
      super.setUp();
   }

   @Test
   public void runTest() throws Throwable {
      IIncludeReference[] includeRefs = cproject.getIncludeReferences();
      assertEquals(1, includeRefs.length);

      IIncludeReference externalFrameworkTestRef = includeRefs[0];
      String expectedExternalFrameworkTestFolderPath = makeExternalResourceAbsolutePath("externalFrameworkTestSubDir");
      assertFolderExists(expectedExternalFrameworkTestFolderPath + "/sub");
      assertEquals(expectedExternalFrameworkTestFolderPath, externalFrameworkTestRef.getPath().toOSString());
   }

   private void assertFolderExists(String expectedFolderName) {
      File folder = new File(expectedFolderName);
      assertTrue(folder.exists());
      assertTrue(folder.isDirectory());
   }
}
