/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 * Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.annotationtest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;
import ch.hsr.ifs.cdttesting.rts.junit4.RunFor;


@RunFor(rtsFile = "/resources/AnnotationTestNotDefaultLocation.rts")
public class AnnotationTest extends CDTTestingTest {

   @Test
   public void runTest() throws Throwable {
      assertEquals("XY.cpp", activeFileName);
      assertEquals("int main() { return 0; }", fileMap.get(activeFileName).getSource());
   }
}
