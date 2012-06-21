/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProjectNature;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.JUnit4RtsTest;

public class CProjectTest extends JUnit4RtsTest {

	@Override
	public void setUp() throws Exception {
		instantiateCCProject = false; // causes to base setup to create a C instead of a C++ project
		super.setUp();
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		IProjectNature cCNature = project.getNature(CCProjectNature.CC_NATURE_ID);
		assertNull(cCNature); // should be null since we created a non-c++ project
		IProjectNature cNature = project.getNature(CProjectNature.C_NATURE_ID);
		assertNotNull(cNature);
	}
}
