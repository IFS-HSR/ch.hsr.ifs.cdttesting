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

import java.util.Arrays;

import org.eclipse.cdt.core.model.IIncludeReference;
import org.junit.Before;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;


public class ReferencedProjectTest extends CDTTestingTest {

   private static final String REFERENCED_PROJECT_NAME1 = "otherProject1";
   private static final String REFERENCED_PROJECT_NAME2 = "otherProject2";

   @Override
   @Before
   public void setUp() throws Exception {
      addReferencedProject(REFERENCED_PROJECT_NAME1, "ReferencedProjectTest_p2.rts");
      addReferencedProject(REFERENCED_PROJECT_NAME2, "ReferencedProjectTest_p3.rts");
      super.setUp();
   }

   @Test
   public void runTest() throws Throwable {
      assertEquals("[P/otherProject1, P/otherProject2]", Arrays.toString(project.getReferencedProjects()));
      IIncludeReference[] inc = cproject.getIncludeReferences();
      assertEquals(2, inc.length);
      assertEquals(makeWorkspaceAbsolutePath(REFERENCED_PROJECT_NAME1), makeOSPath(inc[0].getElementName()));
      assertEquals(makeWorkspaceAbsolutePath(REFERENCED_PROJECT_NAME2), makeOSPath(inc[1].getElementName()));
   }
}
