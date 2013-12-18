/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;

public class SourceFileContentTest extends CDTTestingTest {

	@Override
	@Test
	public void runTest() throws Throwable {
		assertEquals("XY.cpp", activeFileName);
		assertEquals("#include <iostream>" + NL + NL + "int main() { return 0; }", fileMap.get(activeFileName).getSource());
		assertEquals("int main() { return 0; }", fileMap.get(activeFileName).getExpectedSource());
	}
}
