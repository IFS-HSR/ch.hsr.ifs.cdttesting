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

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.base.CDTTestingUITest;


public class SourceFileContentTest extends CDTTestingUITest {

   @Test
   public void runTest() throws Throwable {
      assertEquals("#include <iostream>" + NL + NL + "int main() { return 0; }", testFiles.get("XY.cpp").getSource());
      assertEquals("int main() { return 0; }", testFiles.get("XY.cpp").getExpectedSource());
   }
}
