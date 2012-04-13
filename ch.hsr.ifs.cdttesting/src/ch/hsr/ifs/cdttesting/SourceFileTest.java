/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.cdt.core.tests.BaseTestFramework;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.TextSelection;

public abstract class SourceFileTest extends BaseTestFramework {
	protected static final NullProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
	private static final String CONFIG_FILE_NAME = ".config";

	protected TreeMap<String, TestSourceFile> fileMap;
	protected String fileWithSelection;
	protected TextSelection selection;
	protected String activeFileName;
	protected boolean createCProject = false;

	public SourceFileTest() {
		super();
		fileMap = new TreeMap<String, TestSourceFile>();
	}

	public void initTestSourceFiles(ArrayList<TestSourceFile> files) {
		initActiveFileName(files);
		TestSourceFile configFile = null;
		for (TestSourceFile file : files) {
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

	private void initSelection(ArrayList<TestSourceFile> files) {
		for (TestSourceFile file : files) {
			TextSelection selection = file.getSelection();
			if (selection != null) {
				fileWithSelection = file.getName();
				this.selection = selection;
				break;
			}
		}
	}

	private void initActiveFileName(ArrayList<TestSourceFile> files) {
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

	private void initializeConfiguration(TestSourceFile configFile) {

		Properties properties = new Properties();
		try {
			properties.load(new ByteArrayInputStream(configFile.getSource().getBytes()));
		} catch (final IOException e) {
		}
		initializeConfiguration(properties);
		fileMap.remove(configFile.getName());
	}

	private void initializeConfiguration(Properties properties) {
		initCommonFields(properties);
		configureTest(properties);
	}

	private void initializeConfiguration() {
		initializeConfiguration(new Properties());
	}

	protected void configureTest(Properties properties) {
	};

	private void initCommonFields(Properties properties) {
		String filename = properties.getProperty("filename", null);
		if (filename != null) {
			activeFileName = filename;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		adaptProject();

		for (TestSourceFile testFile : fileMap.values()) {
			importFile(testFile.getName(), testFile.getSource());
		}
	}

	private void adaptProject() {
		if (createCProject) {
			try {
				cleanupProject();
				cproject = CProjectHelper.createCProject("RegressionTestProject", "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ //$NON-NLS-2$
				project = cproject.getProject();
			} catch (CoreException e) {
			} catch (Exception e) {
			}
			if (project == null)
				fail("Unable to create project"); //$NON-NLS-1$
		}
	}

	protected IFile importFile(String fileName, String contents, IProject project) throws Exception {
		IFile file = project.getFile(fileName);
		IPath projectRelativePath = file.getProjectRelativePath();
		for (int i = projectRelativePath.segmentCount() - 1; i > 0; i--) {
			IPath folderPath = file.getProjectRelativePath().removeLastSegments(i);
			IFolder folder = project.getFolder(folderPath);
			if (!folder.exists()) {
				folder.create(false, true, monitor);
			}
		}
		InputStream stream = new ByteArrayInputStream(contents.getBytes());
		if (file.exists())
			file.setContents(stream, false, false, monitor);
		else
			file.create(stream, false, monitor);

		fileManager.addFile(file);
		return file;
	}

	@Override
	protected IFile importFile(String fileName, String contents) throws Exception {
		return importFile(fileName, contents, project);
	}

	@Override
	protected void tearDown() throws Exception {
		fileManager.closeAllFiles();
		super.tearDown();
		cleanupProject();
	}
}
