/*******************************************************************************
 * Copyright (c) 2012 Institute for Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.cdt.core.testplugin.TestScannerProvider;
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
import org.junit.runner.RunWith;

@RunWith(RTSTest.class)
public abstract class JUnit4RtsTest extends SourceFileTest {

	private static final String testRegexp = "//!(.*)\\s*(\\w*)*$";
	private static final String fileRegexp = "//@(.*)\\s*(\\w*)*$";
	private static final String resultRegexp = "//=.*$";

	protected enum MatcherState {
		skip, inTest, inSource, inExpectedResult
	}

	/**
	 * Key: projectName, value: rtsFileName
	 */
	private final LinkedHashMap<String, String> referencedProjectsToLoad;
	private final List<String> externalIncudeDirPaths;
	private final List<String> inProjectIncudeDirPaths;
	protected final ArrayList<ICProject> referencedProjects;

	public JUnit4RtsTest() {
		ExternalResourceHelper.copyPluginResourcesToTestingWorkspace(getClass());
		referencedProjectsToLoad = new LinkedHashMap<String, String>();
		externalIncudeDirPaths = new ArrayList<String>();
		inProjectIncudeDirPaths = new ArrayList<String>();
		referencedProjects = new ArrayList<ICProject>();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		initReferencedProjects();
		setupProjectReferences();
		addIncludePathDirs();
		preSetupIndex();
		setUpIndex();
	}

	protected void preSetupIndex() {
		// do nothing, extending classes can override
	}

	private void setupProjectReferences() throws CoreException {
		if (referencedProjects.size() > 0) {
			ICProjectDescription des = CCorePlugin.getDefault().getProjectDescription(project.getProject(), true);
			ICConfigurationDescription cfgs[] = des.getConfigurations();
			for (ICConfigurationDescription config : cfgs) {
				Map<String, String> refMap = config.getReferenceInfo();
				for (ICProject refProject : referencedProjects) {
					refMap.put(refProject.getProject().getName(), ""); //$NON-NLS-1$
				}
				config.setReferenceInfo(refMap);
			}
			CCorePlugin.getDefault().setProjectDescription(project, des);
		}
	}

	@Override
	@After
	public void tearDown() throws Exception {
		deleteReferencedProjects();
		super.tearDown();
	}

	private void deleteReferencedProjects() {
		for (ICProject curProj : referencedProjects) {
			try {
				curProj.getProject().delete(true, false, monitor);
			} catch (CoreException e) {
				// ignore
			}
		}
		referencedProjects.clear();
	}

	protected void setUpIndex() throws CoreException {
		final int tries = 3;
		int attempt = 0;
		boolean status;
		do {
			doSetUpIndex();
			status = checkTestStatus();
		} while (!status && attempt++ < tries);
		assertTrue("The indexing operation of the test CProject has not finished jet. This should not happen...", status);
	}

	private void doSetUpIndex() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);
		CCorePlugin.getIndexManager().setIndexerId(cproject, IPDOMManager.ID_FAST_INDEXER);
		CCorePlugin.getIndexManager().reindex(cproject);
		for (ICProject curProj : referencedProjects) {
			CCorePlugin.getIndexManager().setIndexerId(curProj, IPDOMManager.ID_FAST_INDEXER);
			CCorePlugin.getIndexManager().reindex(curProj);
		}
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);

		boolean joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, NULL_PROGRESS_MONITOR);
		if (!joined) {
			// Second join due to some strange interruption of JobMonitor when starting unit tests.
			System.err.println("First join on indexer failed. Trying again.");
			joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, NULL_PROGRESS_MONITOR);
			if (!joined) {
				System.err.println("Second join on indexer failed. Trying again.");
			}
		}
	}

	private boolean checkTestStatus() throws CoreException {
		boolean hasFiles = CCorePlugin.getIndexManager().getIndex(cproject).getAllFiles().length != 0;
		if (!hasFiles) {
			System.err.println("Test " + getName() + " is not properly setup and will most likely fail!");
		}
		return hasFiles;
	}

	@RTSTestCases
	public static Map<String, ArrayList<TestSourceFile>> testCases(Class<? extends JUnit4RtsTest> testClass) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(testClass);
		try {
			Map<String, ArrayList<TestSourceFile>> testCases = createTests(rtsFileInfo.getRtsFileReader());
			return testCases;
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

	protected static Map<String, ArrayList<TestSourceFile>> createTests(final BufferedReader inputReader) throws Exception {
		Map<String, ArrayList<TestSourceFile>> testCases = new TreeMap<String, ArrayList<TestSourceFile>>();

		String line;
		ArrayList<TestSourceFile> files = new ArrayList<TestSourceFile>();
		TestSourceFile actFile = null;
		MatcherState matcherState = MatcherState.skip;
		String testName = null;
		boolean bevorFirstTest = true;

		while ((line = inputReader.readLine()) != null) {

			if (lineMatchesBeginOfTest(line)) {
				if (!bevorFirstTest) {
					testCases.put(testName, files);
					files = new ArrayList<TestSourceFile>();
					testName = null;
				}
				matcherState = MatcherState.inTest;
				testName = getNameOfTest(line);
				bevorFirstTest = false;
				continue;
			} else if (lineMatchesBeginOfResult(line)) {
				matcherState = MatcherState.inExpectedResult;
				if (actFile != null) {
					actFile.initExpectedSource();
				}
				continue;
			} else if (lineMatchesFileName(line)) {
				matcherState = MatcherState.inSource;
				actFile = new TestSourceFile(getFileName(line));
				files.add(actFile);
				continue;
			}

			switch (matcherState) {
			case skip:
			case inTest:
				break;
			case inSource:
				if (actFile != null) {
					actFile.addLineToSource(line);
				}
				break;
			case inExpectedResult:
				if (actFile != null) {
					actFile.addLineToExpectedSource(line);
				}
				break;
			}
		}
		testCases.put(testName, files);

		return testCases;
	}

	private static String getFileName(final String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(fileRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return null;
		}
	}

	private static boolean lineMatchesBeginOfTest(final String line) {
		return createMatcherFromString(testRegexp, line).find();
	}

	private static boolean lineMatchesFileName(final String line) {
		return createMatcherFromString(fileRegexp, line).find();
	}

	private static Matcher createMatcherFromString(final String pattern, final String line) {
		return Pattern.compile(pattern).matcher(line);
	}

	private static String getNameOfTest(final String line) {
		Matcher matcherBeginOfTest = createMatcherFromString(testRegexp, line);
		if (matcherBeginOfTest.find()) {
			return matcherBeginOfTest.group(1);
		} else {
			return Messages.getString("IncludatorTester.NotNamed");
		}
	}

	private static boolean lineMatchesBeginOfResult(final String line) {
		return createMatcherFromString(resultRegexp, line).find();
	}

	protected void addReferencedProject(String projectName, String rtsFileName) {
		referencedProjectsToLoad.put(projectName, appendSubPackages(rtsFileName));
	}

	private String appendSubPackages(String rtsFileName) {
		String testClassPackage = getClass().getPackage().getName();
		return testClassPackage + "." + rtsFileName;
	}

	protected void initReferencedProjects() throws Exception {
		for (Entry<String, String> curEntry : referencedProjectsToLoad.entrySet()) {
			initReferencedProject(curEntry.getKey(), curEntry.getValue());
		}
	}

	private void initReferencedProject(String projectName, String rtsFileName) throws Exception {
		RtsFileInfo rtsFileInfo = new RtsFileInfo(rtsFileName);
		try {
			BufferedReader in = rtsFileInfo.getRtsFileReader();
			Map<String, ArrayList<TestSourceFile>> testCases = createTests(in);
			if (testCases.isEmpty()) {
				throw new Exception("Failed to add referenced project. RTS file " + rtsFileName + " does not contain any test-cases.");
			} else if (testCases.size() > 1) {
				throw new Exception("RTS files + " + rtsFileName + " which represents a referenced project must only contain a single test case.");
			}
			ICProject cProj = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER);
			for (TestSourceFile testFile : testCases.values().iterator().next()) {
				if (testFile.getSource().length() > 0) {
					importFile(testFile.getName(), testFile.getSource(), cProj.getProject());
				}
			}
			referencedProjects.add(cProj);
		} finally {
			rtsFileInfo.closeReaderStream();
		}
	}

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
			cproject.setRawPathEntries(newPathEntries, new NullProgressMonitor());
		} catch (CModelException e) {
			e.printStackTrace();
		}
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
