/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProjectNature;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;


public class CProjectTest extends CDTTestingUITest {

   @Test
   public void runTest() throws Throwable {
      IProjectNature cCNature = getCurrentProject().getNature(CCProjectNature.CC_NATURE_ID);
      assertNull(cCNature); // should be null since we created a non-c++ project
      IProjectNature cNature = getCurrentProject().getNature(CProjectNature.C_NATURE_ID);
      assertNotNull(cNature);
   }
}
