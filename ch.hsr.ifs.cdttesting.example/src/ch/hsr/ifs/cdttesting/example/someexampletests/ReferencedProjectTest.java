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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IIncludeReference;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;


public class ReferencedProjectTest extends CDTTestingUITest {

   private static final String REFERENCED_PROJECT_NAME1 = "otherProject1";
   private static final String REFERENCED_PROJECT_NAME2 = "otherProject2";

   @Override
   protected void initReferencedProjects() throws Exception {
      stageReferencedProjectForBothProjects(REFERENCED_PROJECT_NAME1, "ReferencedProjectTest_p2.rts");
      stageReferencedProjectForBothProjects(REFERENCED_PROJECT_NAME2, "ReferencedProjectTest_p3.rts");
      super.initReferencedProjects();
   }

   @Test
   public void runTest() throws Throwable {
      assertEquals("otherProject1, otherProject2", currentProjectHolder.getReferencedProjects().stream().map(ICProject::toString).collect(Collectors
            .joining(", ")));
      IIncludeReference[] inc = getCurrentCProject().getIncludeReferences();
      assertEquals(2, inc.length);
      List<ICProject> referencedProjects = currentProjectHolder.getReferencedProjects();
      assertEquals(referencedProjects.get(0).getProject().getLocation(), inc[0].getPath());
      assertEquals(referencedProjects.get(1).getProject().getLocation(), inc[1].getPath());
   }
}
