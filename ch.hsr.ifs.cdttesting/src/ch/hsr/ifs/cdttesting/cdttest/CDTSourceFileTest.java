/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 *
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.cdttest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.TextSelection;

import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;

public abstract class CDTSourceFileTest extends CDTProjectTest {
	private static final String CONFIG_FILE_NAME = ".config";

	protected TreeMap<String, TestSourceFile> fileMap;
	protected String fileWithSelection;
	protected TextSelection selection;
	protected String activeFileName;

	/**
	 * Key: projectName, value: project's files
	 */
	protected final LinkedHashMap<String, List<TestSourceFile>> referencedProjectsToLoad;

	{
		instantiateExpectedProject = true;
	}

	public CDTSourceFileTest() {
		super();
		fileMap = new TreeMap<>();
		referencedProjectsToLoad = new LinkedHashMap<>();
	}

	public void initTestSourceFiles(final List<TestSourceFile> files) {
		initActiveFileName(files);
		TestSourceFile configFile = null;
		for (final TestSourceFile file : files) {
			fileMap.put(file.getName(), file);
			if (file.getName().equals(CONFIG_FILE_NAME)) {
				configFile = file;
			}
		}
		if (configFile != null) {
			initializeConfiguration(configFile);
		} else {
			initializeConfiguration();
		}
		initSelection(files);
	}

	private void initSelection(final List<TestSourceFile> files) {
		for (final TestSourceFile file : files) {
			final TextSelection selection = file.getSelection();
			if (selection != null) {
				fileWithSelection = file.getName();
				this.selection = selection;
				break;
			}
		}
	}

	private void initActiveFileName(final List<TestSourceFile> files) {
		activeFileName = ".unknown";
		int index = 0;
		if (files.size() <= index) {
			return;
		}
		if (files.get(0).getName().equals(CONFIG_FILE_NAME)) {
			index++;
		}
		if (files.size() > index) {
			activeFileName = files.get(index).getName();
		}
	}

	private void initializeConfiguration(final TestSourceFile configFile) {

		final Properties properties = new Properties();
		try {
			properties.load(new ByteArrayInputStream(configFile.getSource().getBytes()));
		} catch (final IOException e) {
		}
		initializeConfiguration(properties);
		fileMap.remove(configFile.getName());
	}

	private void initializeConfiguration(final Properties properties) {
		initCommonFields(properties);
		configureTest(properties);
	}

	private void initializeConfiguration() {
		initializeConfiguration(new Properties());
	}

	protected void configureTest(final Properties properties) {
	};

	private void initCommonFields(final Properties properties) {
		final String filename = properties.getProperty("filename", null);
		if (filename != null) {
			activeFileName = filename;
		}
	}

	@Override
	protected void setupFiles() throws Exception {
		for (final TestSourceFile testFile : fileMap.values()) {
			importFile(testFile.getName(), testFile.getSource());
			importFile(testFile.getName(), testFile.getExpectedSource(), expectedProject);
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		expectedProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		cleanupProject();
	}

	@Override
	protected void initReferencedProjects() throws Exception {
		for (final Entry<String, List<TestSourceFile>> curEntry : referencedProjectsToLoad.entrySet()) {
			initReferencedProject(curEntry.getKey(), curEntry.getValue());
		}
	}

	private void initReferencedProject(final String projectName, final List<TestSourceFile> testCases)
			throws Exception {
		final ICProject cProj = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER);
		for (final TestSourceFile testFile : testCases) {
			if (testFile.getSource().length() > 0) {
				importFile(testFile.getName(), testFile.getSource(), cProj.getProject());
			}
		}
		referencedProjects.add(cProj);
	}

	protected String getTestSource() throws IOException {
		return getTestSource(activeFileName);
	}

	protected String getTestSource(final String relativeFilePath) throws IOException {
		if (relativeFilePath.startsWith("..")) {
			return getExternalSource(makeProjectAbsolutePath(relativeFilePath));
		}
		return fileMap.get(relativeFilePath).getSource();
	}

	private String getExternalSource(final String absoluteFilePath) throws IOException {
		final int len = (int) (new File(absoluteFilePath).length());
		final FileInputStream fis = new FileInputStream(absoluteFilePath);
		final byte buf[] = new byte[len];
		fis.read(buf);
		fis.close();
		return new String(buf);
	}

	protected String getExpectedSource() {
		return getExpectedSource(activeFileName);
	}

	protected String getExpectedSource(final String relativeFilePath) {
		return fileMap.get(relativeFilePath).getExpectedSource();
	}
}
