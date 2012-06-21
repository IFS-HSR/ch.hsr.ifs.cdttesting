/*******************************************************************************
 * Copyright (c) 2011 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
//@formatter:off
//			ExternalIncludeDependencyTestFrameworkTest.class,
			SourceFileContentTest.class,
			ReferencedProjectTest.class,
			CheckNoUnresolvedInclusionsTest.class,
			ExternalIncludeDependencyTestFrameworkTest.class,
			CProjectTest.class,
			TwoIndexFileForOneFileTest.class,
			//@formatter:on	
})
public class SomeExampleTestsSuite {
}
