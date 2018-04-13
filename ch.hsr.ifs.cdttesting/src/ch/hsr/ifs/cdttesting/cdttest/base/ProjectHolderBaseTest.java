/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * Markus Schorn (Wind River Systems)
 *******************************************************************************/

/*
 * Created on Oct 4, 2004
 */
package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.junit.After;
import org.junit.Before;

import ch.hsr.ifs.iltis.core.functional.functions.Consumer;
import ch.hsr.ifs.iltis.core.functional.functions.Function;

import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.IProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.IProjectHolder.ProjectHolderJob;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.ITestProjectHolder;
import ch.hsr.ifs.cdttesting.cdttest.base.projectholder.ITestProjectHolder.ReferencedProjectDescription;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


abstract public class ProjectHolderBaseTest {

   public static final String NL = System.getProperty("line.separator");

   /**
    * Set this to {@code false} to enforce sequential execution of the project holder operations
    */
   protected boolean executeProjectHolderOperationsParallel = true;

   private boolean calledInitProjectFiles       = false;
   private boolean calledInitReferencedProjects = false;
   private boolean calledInitAdditionalIncludes = false;
   private boolean calledPreSetupIndex          = false;

   protected static String EXTERNAL_TEST_RESOURCE_PROJECT_NAME = "External_Test_Resources_Project";

   protected ITestProjectHolder currentProjectHolder;
   protected ITestProjectHolder expectedProjectHolder;

   protected IProjectHolder externalTestResourcesHolder;

   protected IWorkspace workspace;
   protected String     testName;

   protected String makeExpectedProjectName() {
      return makeProjectNamePrefix() + "_expected_project";
   }

   protected String makeCurrentProjectName() {
      return makeProjectNamePrefix() + "_current_project";
   }

   private String makeProjectNamePrefix() {
      return getName().replaceAll("[^\\w]", "_");
   }

   /**
    * Use to stage additional external include paths to the project holders. The paths have to be relative to the external test resources folder.
    */
   protected void stageExternalIncludePathsForBothProjects(String... paths) {
      IPath[] absolutePaths = Arrays.stream(paths).map(externalTestResourcesHolder::makeProjectAbsolutePath).toArray(IPath[]::new);
      stageExternalIncludePathForBothProjects(absolutePaths);
   }

   /**
    * Use to stage additional external include paths to the project holders. The paths have to be relative to the external test resources folder.
    */
   protected void stageExternalIncludePathForBothProjects(IPath[] absolutePaths) {
      doOnBoth(holder -> holder.stageAbsoluteExternalIncludePaths(absolutePaths));
   }

   /**
    * Use to stage additional project-internal include paths to the project holders. The paths have to be relative to the project folder.
    */
   protected void stageInternalIncludePathsForBothProjects(String... paths) {
      IPath[] relativePaths = Arrays.stream(paths).map(Path::new).toArray(IPath[]::new);
      stageInternalIncludePathsForBothProjects(relativePaths);
   }

   /**
    * Use to stage additional project-internal include paths to the project holders. The paths have to be relative to the project folder.
    */
   protected void stageInternalIncludePathsForBothProjects(IPath[] relativePaths) {
      doOnBoth(holder -> holder.stageInternalIncludePaths(relativePaths));
   }

   /**
    * Use to stage additional referenced projects to the project holders.
    * 
    * @throws InterruptedException
    */
   protected void stageReferencedProjectsForBothProjects(ReferencedProjectDescription... projects) throws InterruptedException {
      doOnBoth(holder -> holder.stageReferencedProjects(projects));
   }

   /**
    * Use to stage the project files to the project holders
    */
   protected void stageTestSourceFileForImportForBothProjects(Collection<TestSourceFile> files) {
      doOnBoth(holder -> holder.stageTestSourceFilesForImport(files));
   }

   /**
    * Gets the name of this TestCase
    *
    * @return the name of the TestCase
    */
   public String getName() {
      return testName;
   }

   public void setName(String testName) {
      this.testName = testName;
   }

   //  @BeforeEach
   @Before
   public void setUp() throws Exception {
      assertTrue(testName != null);
      initExternalTestResourcesHolder();
      initCurrentExpectedProjectHolders();
      initProjectFiles();
      assertAllSuperMethodsCalled(calledInitProjectFiles, "initProjectFiles");
      setupProjectFiles();
      initReferencedProjects();
      assertAllSuperMethodsCalled(calledInitReferencedProjects, "initReferencedProjects");
      setupReferencedProjects();
      setupProjectReferences();
      initAdditionalIncludes();
      assertAllSuperMethodsCalled(calledInitAdditionalIncludes, "initAdditionalIncludes");
      setupIncludePaths();
      preSetupIndex();
      assertAllSuperMethodsCalled(calledPreSetupIndex, "preSetupIndex");
      setupIndices();
   }

   void assertAllSuperMethodsCalled(boolean guard, String methodName) {
      assertTrue(notAllSuperMethodsWereCalled(methodName), guard);
   }

   private static String notAllSuperMethodsWereCalled(String methodName) {
      return "The overridden method \"" + methodName + "\" does not call super." + methodName + "()!";
   }

   /**
    * Can be overloaded to initialize {@link #externalTestResourcesHolder} and subsequently load external test resources. This should be done by
    * calling
    * {@link IProjectHolder#stageFilesForImport(Enumeration)} or {@link IProjectHolder#stageFilesForImport(Collection)}
    * 
    * @throws Exception
    * 
    * @noreference Do not call this method directly
    */
   protected void initExternalTestResourcesHolder() throws Exception {}

   /**
    * Can be overloaded to instantiate different types of project holder i.e. a fake project holder to gain performance.
    * <p>
    * {@link #initExternalTestResourcesHolder()} must finish executing before calling this method.
    * 
    * @throws Exception
    * 
    * @noreference Do not call this method directly
    */
   protected abstract void initCurrentExpectedProjectHolders() throws Exception;

   /**
    * Override to populate the current- and expected-project with files. This should be done by calling
    * {@link #stageTestSourceFileForImportForBothProjects(Collection)}
    * <p>
    * {@link #initCurrentExpectedProjectHolders()} must finish executing before calling this method.
    * 
    * @throws Exception
    * 
    * @noreference Do not call this method directly
    */
   protected void initProjectFiles() throws Exception {
      this.calledInitProjectFiles = true;
   }

   /**
    * Override to stage referenced projects. This should be done by calling
    * {@link #stageReferencedProjectsForBothProjects(ReferencedProjectDescription...)}
    * <p>
    * {@link #initCurrentExpectedProjectHolders()} must finish executing before calling this method.
    * 
    * @throws Exception
    * 
    * @noreference Do not call this method directly
    */
   protected void initReferencedProjects() throws Exception {
      this.calledInitReferencedProjects = true;
   }

   /**
    * Override to add additional includes. This should be done by calling
    * {@link #stageExternalIncludePathsForBothProjects(String...)} or {@link #stageInternalIncludePathsForBothProjects(String...)}
    * <p>
    * {@link #initCurrentExpectedProjectHolders()} must finish executing before calling this method.
    * 
    * @throws Exception
    * 
    * @noreference Do not call this method directly
    */
   protected void initAdditionalIncludes() throws Exception {
      this.calledInitAdditionalIncludes = true;
   }

   private void setupProjectFiles() throws Exception {
      scheduleAndJoinBoth(ITestProjectHolder::importFilesAsync);
   }

   private void setupReferencedProjects() throws InterruptedException {
      scheduleAndJoinBoth(ITestProjectHolder::setupReferencedProjectsAsync);
   }

   private void setupProjectReferences() throws Exception {
      scheduleAndJoinBoth(ITestProjectHolder::setupProjectReferencesAsync);
   }

   private void setupIncludePaths() throws Exception {
      scheduleAndJoinBoth(ITestProjectHolder::setupIncludePathsAsync);
   }

   /**
    * Override to execute additional actions before setting up the indices.
    * 
    * @noreference Do not call this method directly
    */
   protected void preSetupIndex() {
      this.calledPreSetupIndex = true;
   }

   private void setupIndices() throws Exception {
      scheduleAndJoinBoth(ITestProjectHolder::setupIndexAsync);
   }

   //	@AfterEach
   @After
   public void tearDown() throws Exception {
      //      TestScannerProvider.clear(); //TODO(Tobias Stauber) clean after testing
      disposeCDTAstCache();
      cleanupProjects();
   }

   @SuppressWarnings("restriction")
   private void disposeCDTAstCache() {
      CUIPlugin.getDefault().getASTProvider().dispose();
   }

   public void cleanupProjects() throws InterruptedException {
      scheduleAndJoinBoth(ITestProjectHolder::cleanupProjectsAsync);
   }

   /**
    * Convenience method to get the current document from an URI
    */
   protected IDocument getCurrentDocument(URI uri) {
      return currentProjectHolder.getDocument(uri);
   }

   /**
    * Convenience method to get the expected document from an URI
    */
   protected IDocument getExpectedDocument(URI uri) {
      return expectedProjectHolder.getDocument(uri);
   }

   /**
    * Convenience method to get the current document from an IFile
    */
   protected IDocument getCurrentDocument(IFile file) {
      return currentProjectHolder.getDocument(file);
   }

   /**
    * Convenience method to get the expected document from an IFile
    */
   protected IDocument getExpectedDocument(IFile file) {
      return expectedProjectHolder.getDocument(file);
   }

   /**
    * Convenience method to get ICProject of currentProjectHolder
    */
   protected ICProject getCurrentCProject() {
      return currentProjectHolder.getCProject();
   }

   /**
    * Convenience method to get ICProject of expectedProjectHolder
    */
   protected ICProject getExpectedCProject() {
      return expectedProjectHolder.getCProject();
   }

   /**
    * Convenience method to get IProject of currentProjectHolder
    */
   protected IProject getCurrentProject() {
      return currentProjectHolder.getProject();
   }

   /**
    * Convenience method to get IProject of expectedProjectHolder
    */
   protected IProject getExpectedProject() {
      return expectedProjectHolder.getProject();
   }

   /**
    * Convenience method to get ICElement represented by this path in currentProjectHolder
    */
   protected Optional<ICElement> getCurrentCElement(IPath path) {
      return currentProjectHolder.getCElement(path);
   }

   /**
    * Convenience method to get ICElement represented by this file in currentProjectHolder
    */
   protected Optional<ICElement> getCurrentCElement(IFile file) {
      return currentProjectHolder.getCElement(file);
   }

   /**
    * Convenience method to get ICElement represented by this path in expectedProjectHolder
    */
   protected Optional<ICElement> getExpectedCElement(IPath path) {
      return expectedProjectHolder.getCElement(path);
   }

   /**
    * Convenience method to get ICElement represented by this file in expectedProjectHolder
    */
   protected Optional<ICElement> getExpectedCElement(IFile file) {
      return expectedProjectHolder.getCElement(file);
   }

   public void doOnBoth(Consumer<ITestProjectHolder> consumer) {
      consumer.accept(currentProjectHolder);
      consumer.accept(expectedProjectHolder);
   }

   public void scheduleAndJoinBoth(Function<ITestProjectHolder, ProjectHolderJob> supplier) throws InterruptedException {
      scheduleAndJoinBoth(supplier.apply(currentProjectHolder), supplier.apply(expectedProjectHolder), !executeProjectHolderOperationsParallel);
   }

   public static void scheduleAndJoinBoth(Job currentJob, Job expectedJob, boolean executeSequential) throws InterruptedException {
      if (executeSequential) {
         currentJob.schedule();
         currentJob.join();
         expectedJob.schedule();
         expectedJob.join();
      } else {
         currentJob.schedule();
         expectedJob.schedule();
         currentJob.join();
         expectedJob.join();
      }
   }
}
