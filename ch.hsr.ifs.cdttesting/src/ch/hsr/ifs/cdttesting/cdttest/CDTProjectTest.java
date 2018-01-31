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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
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
import org.eclipse.cdt.core.testplugin.util.BaseTestCase;
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
import org.junit.After;
import org.junit.Before;

import ch.hsr.ifs.cdttesting.helpers.ExternalResourceHelper;
import ch.hsr.ifs.cdttesting.helpers.UIThreadSyncRunnable;

abstract public class CDTProjectTest {
	protected static final String EXPECTED_PREFIX = "expected_";

	protected IWorkspace workspace;
	protected IProject project;
	protected ICProject cproject;

	protected IProject expectedProject;
	protected ICProject expectedCproject;
	
	protected String name;

	protected FileManager fileManager;
	protected boolean indexDisabled = false;
	/**
	 * If set to false, a C project will be created instead of a (default) C++
	 * project
	 */
	protected boolean instantiateCCProject = true;

	/**
	 * If set to true, a project supporting the expected files will be
	 * instantiated.
	 */
	protected boolean instantiateExpectedProject = false;

	protected ArrayList<ICProject> referencedProjects;
	private List<String> externalIncudeDirPaths;
	private List<String> inProjectIncudeDirPaths;

	public CDTProjectTest() {
		init();
	}

	public CDTProjectTest(final String name) {
	//		super(name);
		this.name = name;
		init();
	}

	private void init() {
		referencedProjects = new ArrayList<>();
		externalIncudeDirPaths = new ArrayList<>();
		inProjectIncudeDirPaths = new ArrayList<>();
	}

    /**
     * Gets the name of this TestCase
     *
     * @return the name of the TestCase
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of this TestCase
     */
    public void setName(String name) {
        this.name = name;
    }
    
    @Deprecated
    public void runTest() throws Throwable{
    	// THIS IS JUST HERE SO THE @Overload ANNOTATIONS IN POTENTIAL SUBCLASSES WONT BE A PROBLEM.
    }
    
//  @BeforeEach
	@Before
	protected void setUp() throws Exception {
		initProject();
		setupFiles();
		initReferencedProjects();
		setupProjectReferences();
		addIncludePathDirs();
		preSetupIndex();
		setUpIndex();
	}

	protected abstract void setupFiles() throws Exception;

//	@AfterEach
	@After
	protected void tearDown() throws Exception {
		closeOpenEditors();
		TestScannerProvider.clear();
		deleteReferencedProjects();
		
		
		fileManager.closeAllFiles();
		disposeProjMembers();
		disposeCDTAstCache();
	}

	private void initProject() {
		if (project != null && expectedProject != null) {
			return;
		}
		if (CCorePlugin.getDefault() != null && CCorePlugin.getDefault().getCoreModel() != null) {
			final String projectName = makeProjectName();
			workspace = ResourcesPlugin.getWorkspace();
			try {
				if (instantiateCCProject) {
					cproject = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ //$NON-NLS-2$
					if (instantiateExpectedProject) {
						expectedCproject = CProjectHelper.createCCProject(EXPECTED_PREFIX.concat(projectName), "bin", //$NON-NLS-1$ //$NON-NLS-2$
								IPDOMManager.ID_NO_INDEXER);
					}
				} else {
					cproject = CProjectHelper.createCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ //$NON-NLS-2$
					if (instantiateExpectedProject) {
						expectedCproject = CProjectHelper.createCProject(EXPECTED_PREFIX.concat(projectName), "bin", //$NON-NLS-1$ //$NON-NLS-2$
								IPDOMManager.ID_NO_INDEXER);
					}
				}
				project = cproject.getProject();
				if (instantiateExpectedProject) {
					expectedProject = expectedCproject.getProject();
				}
			} catch (final CoreException ignored) {
			}
			if (project == null || (instantiateExpectedProject && expectedProject == null)) {
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
			project.delete(true, true, new NullProgressMonitor());
			expectedProject.delete(true, true, new NullProgressMonitor());
		} catch (final Throwable ignored) {
		} finally {
			project = null;
			expectedProject = null;
		}
	}

	private void disposeProjMembers() throws CoreException {
		disposeProjMembers(project);
		disposeProjMembers(expectedProject);
	}

	private void disposeProjMembers(final IProject proj) throws CoreException {
		if (proj == null || !proj.exists()) {
			return;
		}

		final IResource[] members = proj.members();
		for (final IResource member : members) {
			if (member.getName().equals(".project") || member.getName().equals(".cproject")) {
				continue;
			}
			if (member.getName().equals(".settings")) {
				continue;
			}
			try {
				member.delete(false, new NullProgressMonitor());
			} catch (final Throwable ignored) {
			}
		}
	}

	protected void preSetupIndex() {
		// do nothing, extending classes can override
	}

	private void setupProjectReferences() throws CoreException {
		if (referencedProjects.size() > 0) {
			final ICProjectDescription des = CCorePlugin.getDefault().getProjectDescription(project, true);
			final ICConfigurationDescription cfgs[] = des.getConfigurations();
			for (final ICConfigurationDescription config : cfgs) {
				final Map<String, String> refMap = config.getReferenceInfo();
				for (final ICProject refProject : referencedProjects) {
					refMap.put(refProject.getProject().getName(), "");
				}
				config.setReferenceInfo(refMap);
			}
			CCorePlugin.getDefault().setProjectDescription(project, des);
		}
	}

	private void setUpIndex() throws CoreException {
		disposeCDTAstCache();
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// reindexing will happen automatically after call of setIndexerId
		CCorePlugin.getIndexManager().setIndexerId(cproject, IPDOMManager.ID_FAST_INDEXER);
		for (final ICProject curProj : referencedProjects) {
			CCorePlugin.getIndexManager().setIndexerId(curProj, IPDOMManager.ID_FAST_INDEXER);
		}
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		boolean joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor());
		if (!joined) {
			System.err.println("Join on indexer failed. " + getName() + "might fail.");
			joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor());
			if (!joined) {
				System.err.println("Second join on indexer failed.");
			}
		}
		try {
			BaseTestCase.waitForIndexer(cproject);
		} catch (final InterruptedException e) {
			System.err.println("Wait for indexer has been interrupted.");
		}
	}

	protected abstract void initReferencedProjects() throws Exception;

	protected void addIncludeDirPath(final String path) {
		externalIncudeDirPaths.add(path);
	}

	protected void addInProjectIncludeDirPath(final String projectRelativePath) {
		inProjectIncudeDirPaths.add(projectRelativePath);
	}

	private void addIncludePathDirs() {
		final int externalProjectOffset = externalIncudeDirPaths.size() + inProjectIncudeDirPaths.size();
		final String[] array = new String[externalProjectOffset + referencedProjects.size()];
		int i = 0;
		for (; i < externalIncudeDirPaths.size(); i++) {
			final String externalAbsolutePath = makeExternalResourceAbsolutePath(externalIncudeDirPaths.get(i));
			final File folder = new File(externalAbsolutePath);
			if (!folder.exists()) {
				System.err.println("Adding external include path dir " + externalAbsolutePath + " to test " + getName()
						+ " which does not exist.");
			}
			array[i] = externalAbsolutePath;
		}
		for (; i < externalProjectOffset; i++) {
			final String inProjectAbsolutePath = makeProjectAbsolutePath(
					inProjectIncudeDirPaths.get(i - externalIncudeDirPaths.size()));
			final File folder = new File(inProjectAbsolutePath);
			if (!folder.exists()) {
				System.err.println("Adding external include path dir " + inProjectAbsolutePath + " to test " + getName()
						+ " which does not exist.");
			}
			array[i] = inProjectAbsolutePath;
		}
		for (; i < array.length; i++) {
			final ICProject referencedProj = referencedProjects.get(i - externalProjectOffset);
			array[i] = referencedProj.getProject().getLocation().toOSString();
		}
		externalIncudeDirPaths.clear();
		inProjectIncudeDirPaths.clear();
		addIncludeRefs(array, externalProjectOffset);
		TestScannerProvider.sIncludes = array;
	}

	private void addIncludeRefs(final String[] pathsToAdd, final int externalProjectOffset) {
		try {
			final IPathEntry[] allPathEntries = cproject.getRawPathEntries();
			final IPathEntry[] newPathEntries = new IPathEntry[allPathEntries.length + pathsToAdd.length];
			System.arraycopy(allPathEntries, 0, newPathEntries, 0, allPathEntries.length);
			int i = 0;
			for (; i < externalProjectOffset; i++) {
				newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null, null,
						new Path(pathsToAdd[i]));
			}
			for (; i < pathsToAdd.length; i++) {
				final ICProject referencedProj = referencedProjects.get(i - externalProjectOffset);
				newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null,
						referencedProj.getPath().makeRelative(), null);
			}
			cproject.setRawPathEntries(newPathEntries, new NullProgressMonitor());
		} catch (final CModelException e) {
			e.printStackTrace();
		}
	}

	protected IFile importFile(final String fileName, final String contents) throws Exception {
		return importFile(fileName, contents, project);
	}

	protected IFile importFile(final String fileName, final String contents, final IProject project) throws Exception {
		final IFile file = project.getFile(fileName);
		final IPath projectRelativePath = file.getProjectRelativePath();
		for (int i = projectRelativePath.segmentCount() - 1; i > 0; i--) {
			final IPath folderPath = file.getProjectRelativePath().removeLastSegments(i);
			final IFolder folder = project.getFolder(folderPath);
			if (!folder.exists()) {
				folder.create(false, true, new NullProgressMonitor());
			}
		}
		final InputStream stream = new ByteArrayInputStream(contents.getBytes());
		if (file.exists()) {
			System.err.println("Overwriting existing file which should not yet exist: " + fileName);
			file.setContents(stream, true, false, new NullProgressMonitor());
		} else {
			file.create(stream, true, new NullProgressMonitor());
		}

		fileManager.addFile(file);
		checkFileContent(file.getLocation(), contents);
		return file;
	}

	private void checkFileContent(final IPath location, final String expected) throws IOException {
		Reader in = null;
		try {
			in = new FileReader(location.toOSString());
			final StringBuilder existing = new StringBuilder();
			final char[] buffer = new char[4096];
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

	@SuppressWarnings("restriction")
	private void disposeCDTAstCache() {
		CUIPlugin.getDefault().getASTProvider().dispose();
	}

	protected String makeExternalResourceAbsolutePath(final String relativePath) {
		return ExternalResourceHelper.makeExternalResourceAbsolutePath(relativePath);
	}

	protected String makeProjectAbsolutePath(final String relativePath) {
		return makeProjectAbsolutePath(relativePath, project);
	}

	protected String makeProjectAbsolutePath(final String relativePath, final IProject proj) {
		final IPath projectPath = proj.getLocation();
		return projectPath.append(relativePath).toOSString();
	}

	protected String makeWorkspaceAbsolutePath(final String relativePath) {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(relativePath).toOSString();
	}

	protected String makeOSPath(final String path) {
		return new Path(path).toOSString();
	}

	private void deleteReferencedProjects() {
		for (final ICProject curProj : referencedProjects) {
			try {
				curProj.getProject().delete(true, false, new NullProgressMonitor());
			} catch (final CoreException ignore) {
			}
		}
		referencedProjects.clear();
	}

	protected IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			final IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
			assertEquals("There should be exactly one workbench window. Includator test will thus fail.", 1,
					workbenchWindows.length);
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
