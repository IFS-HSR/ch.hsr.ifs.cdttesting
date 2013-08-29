/*******************************************************************************
 * Copyright (c) 2011 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ch.hsr.ifs.cdttesting.example.annotationtest.SomeExampleAnnotationTestsSuite;
import ch.hsr.ifs.cdttesting.example.someexampletests.SomeExampleTestsSuite;

@RunWith(Suite.class)
@SuiteClasses({
//@formatter:off
	SomeExampleTestsSuite.class,
	SomeExampleAnnotationTestsSuite.class,
	//@formatter:on
})
public class TestSuiteAll {
}
