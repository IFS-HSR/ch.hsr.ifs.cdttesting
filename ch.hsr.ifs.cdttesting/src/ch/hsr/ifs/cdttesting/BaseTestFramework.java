/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/

/*
 * Created on Oct 4, 2004
 */
package ch.hsr.ifs.cdttesting;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.cdt.core.testplugin.FileManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author aniefer
 */
abstract public class BaseTestFramework extends TestCase {
	protected NullProgressMonitor monitor;
	protected IWorkspace workspace;
	protected IProject project;
	protected ICProject cproject;
	protected FileManager fileManager;
	protected boolean indexDisabled = false;
	/**
	 * when set to false, a C project will be created instead of a (default) C++ project
	 */
	protected boolean instantiateCCProject = true;

	void initProject() {
		if (project != null) {
			return;
		}
		if (CCorePlugin.getDefault() != null && CCorePlugin.getDefault().getCoreModel() != null) {
			String projectName = makeProjectName();
			monitor = new NullProgressMonitor();
			workspace = ResourcesPlugin.getWorkspace();
			try {
				if (instantiateCCProject) {
					cproject = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					cproject = CProjectHelper.createCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ //$NON-NLS-2$
				}
				project = cproject.getProject();
			} catch (CoreException e) {
				/* boo */
			}
			if (project == null) {
				fail("Unable to create project"); //$NON-NLS-1$
			}
			fileManager = new FileManager();
		}
	}

	private String makeProjectName() {
		return getName().replaceAll("[^\\w]", "_") + "_project";
	}

	public BaseTestFramework() {
		super();
	}

	/**
	 * @param name
	 */
	public BaseTestFramework(String name) {
		super(name);
	}

	public void cleanupProject() throws Exception {
		try {
			project.delete(true, false, monitor);
		} catch (Throwable e) {
			/* boo */
		} finally {
			project = null;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initProject();
	}

	@Override
	protected void tearDown() throws Exception {
		if (project == null || !project.exists())
			return;

		IResource[] members = project.members();
		for (int i = 0; i < members.length; i++) {
			if (members[i].getName().equals(".project") || members[i].getName().equals(".cproject")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			if (members[i].getName().equals(".settings"))
				continue;
			try {
				members[i].delete(false, monitor);
			} catch (Throwable e) {
				/* boo */
			}
		}
	}

	protected IFile importFile(String fileName, String contents) throws Exception {
		IFile file = project.getProject().getFile(fileName);
		InputStream stream = new ByteArrayInputStream(contents.getBytes());
		if (file.exists()) {
			file.setContents(stream, false, false, monitor);
		} else {
			IPath path = file.getLocation();
			path = path.makeRelativeTo(project.getLocation());
			if (path.segmentCount() > 1) {
				path = path.removeLastSegments(1);

				for (int i = path.segmentCount() - 1; i >= 0; i--) {
					IPath currentPath = path.removeLastSegments(i);
					IFolder folder = project.getFolder(currentPath);
					if (!folder.exists()) {
						folder.create(false, true, null);
					}
				}
			}
			file.create(stream, false, monitor);
		}
		fileManager.addFile(file);
		return file;
	}
}
