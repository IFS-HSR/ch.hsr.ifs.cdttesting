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
package ch.hsr.ifs.cdttesting.cdttest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.cdt.core.testplugin.FileManager;
import org.eclipse.cdt.core.testplugin.TestScannerProvider;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;

/**
 * @author aniefer
 */
abstract public class CDTProjectTest extends TestCase {
	protected static final NullProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
	protected IWorkspace workspace;
	protected IProject project;
	protected ICProject cproject;
	protected FileManager fileManager;
	protected boolean indexDisabled = false;
	/**
	 * when set to false, a C project will be created instead of a (default) C++ project
	 */
	protected boolean instantiateCCProject = true;

	protected ArrayList<ICProject> referencedProjects;
	private List<String> externalIncudeDirPaths;
	private List<String> inProjectIncudeDirPaths;

	public CDTProjectTest() {
		init();
	}

	public CDTProjectTest(String name) {
		super(name);
		init();
	}

	private void init() {
		referencedProjects = new ArrayList<ICProject>();
		externalIncudeDirPaths = new ArrayList<String>();
		inProjectIncudeDirPaths = new ArrayList<String>();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initProject();
		setupFiles();
		initReferencedProjects();
		setupProjectReferences();
		addIncludePathDirs();
		preSetupIndex();
		setUpIndex();
		checkTestStatus();
	}

	protected abstract void setupFiles() throws Exception;

	@Override
	protected void tearDown() throws Exception {
		closeOpenEditors();
		TestScannerProvider.clear();
		deleteReferencedProjects();
		fileManager.closeAllFiles();
		disposeProjMembers();
		disposeCDTAstCache();
	}

	private void initProject() {
		if (project != null) {
			return;
		}
		if (CCorePlugin.getDefault() != null && CCorePlugin.getDefault().getCoreModel() != null) {
			String projectName = makeProjectName();
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

	public void cleanupProject() throws Exception {
		try {
			project.delete(true, false, NULL_PROGRESS_MONITOR);
		} catch (Throwable e) {
			/* boo */
		} finally {
			project = null;
		}
	}

	private void disposeProjMembers() throws CoreException {
		if (project == null || !project.exists())
			return;

		IResource[] members = project.members();
		for (int i = 0; i < members.length; i++) {
			if (members[i].getName().equals(".project") || members[i].getName().equals(".cproject")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			if (members[i].getName().equals(".settings"))
				continue;
			try {
				members[i].delete(false, NULL_PROGRESS_MONITOR);
			} catch (Throwable e) {
				/* boo */
			}
		}
	}

	private boolean checkTestStatus() throws CoreException {
		IIndex index = CCorePlugin.getIndexManager().getIndex(cproject);
		boolean hasFiles = false;
		try {
			index.acquireReadLock();
			hasFiles = index.getAllFiles().length != 0;
			if (!hasFiles) {
				System.err.println("Test " + getName() + " is not properly setup and will most likely fail!");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		} finally {
			index.releaseReadLock();
		}
		return hasFiles;
	}

	protected void preSetupIndex() {
		// do nothing, extending classes can override
	}

	private void setupProjectReferences() throws CoreException {
		if (referencedProjects.size() > 0) {
			ICProjectDescription des = CCorePlugin.getDefault().getProjectDescription(project, true);
			ICConfigurationDescription cfgs[] = des.getConfigurations();
			for (ICConfigurationDescription config : cfgs) {
				Map<String, String> refMap = config.getReferenceInfo();
				for (ICProject refProject : referencedProjects) {
					refMap.put(refProject.getProject().getName(), "");
				}
				config.setReferenceInfo(refMap);
			}
			CCorePlugin.getDefault().setProjectDescription(project, des);
		}
	}

	private void setUpIndex() throws CoreException {
		disposeCDTAstCache();
		project.refreshLocal(IResource.DEPTH_INFINITE, NULL_PROGRESS_MONITOR);
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);
		// reindexing will happen automatically after call of setIndexerId
		CCorePlugin.getIndexManager().setIndexerId(cproject, IPDOMManager.ID_FAST_INDEXER);
		for (ICProject curProj : referencedProjects) {
			CCorePlugin.getIndexManager().setIndexerId(curProj, IPDOMManager.ID_FAST_INDEXER);
		}
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);

		boolean joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, NULL_PROGRESS_MONITOR);
		if (!joined) {
			System.err.println("Join on indexer failed. " + getName() + "might fail.");
			joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, NULL_PROGRESS_MONITOR);
			if (!joined) {
				System.err.println("Second join on indexer failed.");
			}
		}
	}

	protected abstract void initReferencedProjects() throws Exception;

	protected void addIncludeDirPath(String path) {
		externalIncudeDirPaths.add(path);
	}

	protected void addInProjectIncludeDirPath(String projectRelativePath) {
		inProjectIncudeDirPaths.add(projectRelativePath);
	}

	private void addIncludePathDirs() {
		int externalProjectOffset = externalIncudeDirPaths.size() + inProjectIncudeDirPaths.size();
		String[] array = new String[externalProjectOffset + referencedProjects.size()];
		int i = 0;
		for (; i < externalIncudeDirPaths.size(); i++) {
			String externalAbsolutePath = makeExternalResourceAbsolutePath(externalIncudeDirPaths.get(i));
			File folder = new File(externalAbsolutePath);
			if (!folder.exists()) {
				System.err.println("Adding external include path dir " + externalAbsolutePath + " to test " + getName() + " which does not exist.");
			}
			array[i] = externalAbsolutePath;
		}
		for (; i < externalProjectOffset; i++) {
			String inProjectAbsolutePath = makeProjectAbsolutePath(inProjectIncudeDirPaths.get(i - externalIncudeDirPaths.size()));
			File folder = new File(inProjectAbsolutePath);
			if (!folder.exists()) {
				System.err.println("Adding external include path dir " + inProjectAbsolutePath + " to test " + getName() + " which does not exist.");
			}
			array[i] = inProjectAbsolutePath;
		}
		for (; i < array.length; i++) {
			ICProject referencedProj = referencedProjects.get(i - externalProjectOffset);
			array[i] = referencedProj.getProject().getLocation().toOSString();
		}
		externalIncudeDirPaths.clear();
		inProjectIncudeDirPaths.clear();
		addIncludeRefs(array, externalProjectOffset);
		TestScannerProvider.sIncludes = array;
	}

	private void addIncludeRefs(String[] pathsToAdd, int externalProjectOffset) {
		try {
			IPathEntry[] allPathEntries = cproject.getRawPathEntries();
			IPathEntry[] newPathEntries = new IPathEntry[allPathEntries.length + pathsToAdd.length];
			System.arraycopy(allPathEntries, 0, newPathEntries, 0, allPathEntries.length);
			int i = 0;
			for (; i < externalProjectOffset; i++) {
				newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null, null, new Path(pathsToAdd[i]));
			}
			for (; i < pathsToAdd.length; i++) {
				ICProject referencedProj = referencedProjects.get(i - externalProjectOffset);
				newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null, referencedProj.getPath().makeRelative(), null);
			}
			cproject.setRawPathEntries(newPathEntries, NULL_PROGRESS_MONITOR);
		} catch (CModelException e) {
			e.printStackTrace();
		}
	}

	protected IFile importFile(String fileName, String contents, IProject project) throws Exception {
		IFile file = project.getFile(fileName);
		IPath projectRelativePath = file.getProjectRelativePath();
		for (int i = projectRelativePath.segmentCount() - 1; i > 0; i--) {
			IPath folderPath = file.getProjectRelativePath().removeLastSegments(i);
			IFolder folder = project.getFolder(folderPath);
			if (!folder.exists()) {
				folder.create(false, true, NULL_PROGRESS_MONITOR);
			}
		}
		InputStream stream = new ByteArrayInputStream(contents.getBytes());
		if (file.exists()) {
			System.err.println("Overwriding existing file which should not yet exist: " + fileName);
			file.setContents(stream, true, false, NULL_PROGRESS_MONITOR);
		} else
			file.create(stream, true, NULL_PROGRESS_MONITOR);

		fileManager.addFile(file);
		checkFileContent(file.getLocation(), contents);
		return file;
	}

	private void checkFileContent(IPath location, String expected) throws IOException {
		Reader in = null;
		try {
			in = new FileReader(location.toOSString());
			StringBuilder existing = new StringBuilder();
			char[] buffer = new char[4096];
			int read = 0;
			do {
				existing.append(buffer, 0, read);
				read = in.read(buffer);
			} while (read >= 0);
			if (!expected.equals(existing.toString())) {
				System.err.println("file " + location + " not yet written.");
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	protected IFile importFile(String fileName, String contents) throws Exception {
		return importFile(fileName, contents, project);
	}

	@SuppressWarnings("restriction")
	private void disposeCDTAstCache() {
		CUIPlugin.getDefault().getASTProvider().dispose();
	}

	protected String makeExternalResourceAbsolutePath(String relativePath) {
		return ExternalResourceHelper.makeExternalResourceAbsolutePath(relativePath);
	}

	protected String makeProjectAbsolutePath(String relativePath) {
		IPath projectPath = project.getLocation();
		return projectPath.append(relativePath).toOSString();
	}

	protected String makeWorkspaceAbsolutePath(String relativePath) {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(relativePath).toOSString();
	}

	private void deleteReferencedProjects() {
		for (ICProject curProj : referencedProjects) {
			try {
				curProj.getProject().delete(true, false, NULL_PROGRESS_MONITOR);
			} catch (CoreException e) {
				// ignore
			}
		}
		referencedProjects.clear();
	}

	protected IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
			assertEquals("There should be exactly one workbench window. Includator test will thus fail.", 1, workbenchWindows.length);
			activeWorkbenchWindow = workbenchWindows[0];
		}
		return activeWorkbenchWindow;
	}

	protected void closeOpenEditors() throws Exception {
		new UIThreadSyncRunnable() {

			@Override
			protected void runSave() throws Exception {
				getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
			}
		}.runSyncOnUIThread();
	}
}
