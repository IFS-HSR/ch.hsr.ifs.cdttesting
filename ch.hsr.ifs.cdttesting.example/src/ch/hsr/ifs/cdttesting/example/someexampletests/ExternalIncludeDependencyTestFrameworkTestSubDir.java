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
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;


public class ExternalIncludeDependencyTestFrameworkTestSubDir extends CDTTestingUITest {

   @Override
   protected void initAdditionalIncludes() throws Exception {
      stageExternalIncludePathsForBothProjects("externalFrameworkTestSubDir");
      super.initAdditionalIncludes();
   }

   @Test
   public void runTest() throws Throwable {
      IIncludeReference[] includeRefs = getCurrentCProject().getIncludeReferences();
      assertEquals(1, includeRefs.length);

      IIncludeReference externalFrameworkTestRef = includeRefs[0];
      IPath expectedExternalFrameworkTestFolderPath = externalTestResourcesHolder.makeProjectAbsolutePath("externalFrameworkTestSubDir");
      assertFolderExists(expectedExternalFrameworkTestFolderPath.append("/sub"));
      assertEquals(expectedExternalFrameworkTestFolderPath, externalFrameworkTestRef.getPath().toOSString());
   }

   private void assertFolderExists(IPath expectedFolderName) {
      File folder = expectedFolderName.toFile();
      assertTrue(folder.exists());
      assertTrue(folder.isDirectory());
   }
}
