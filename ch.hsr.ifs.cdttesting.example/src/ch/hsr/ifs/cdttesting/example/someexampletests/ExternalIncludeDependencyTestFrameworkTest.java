/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.example.someexampletests;

import java.io.File;

import org.eclipse.cdt.core.model.IIncludeReference;
import org.junit.Before;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.JUnit4RtsTest;

public class ExternalIncludeDependencyTestFrameworkTest extends JUnit4RtsTest {

	@Override
	@Before
	public void setUp() throws Exception {
		addIncludeDirPath("externalFrameworkTest");
		super.setUp();
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		final IIncludeReference[] includeRefs = cproject.getIncludeReferences();
		assertEquals(2, includeRefs.length);

		IIncludeReference externalFrameworkTestRef = includeRefs[0];
		String expectedExternalFrameworkTestFolderPath = makeExternalResourceAbsolutePath("externalFrameworkTest");
		assertFolderExists(expectedExternalFrameworkTestFolderPath);
		assertEquals(expectedExternalFrameworkTestFolderPath, externalFrameworkTestRef.getPath().toOSString());

		IIncludeReference indexerParseUpfrontFakeLibRef = includeRefs[1];
		String expectedParseUpfrontFakeLibFolderPath = makeExternalResourceAbsolutePath("indexerParseUpfrontFakeLib");
		String warning = "Folder 'externalTestResource/indexerParseUpfrontFakeLib (and its content) should always exist, so so that indexer works correctly.";
		assertFolderExists(warning, expectedParseUpfrontFakeLibFolderPath);
		assertEquals(expectedParseUpfrontFakeLibFolderPath, indexerParseUpfrontFakeLibRef.getPath().toOSString());
	}

	private void assertFolderExists(String expectedFolderName) {
		assertFolderExists(null, expectedFolderName);
	}

	private void assertFolderExists(String message, String expectedFolderName) {
		File folder = new File(expectedFolderName);
		assertTrue(message, folder.exists());
		assertTrue(folder.isDirectory());
	}
}
