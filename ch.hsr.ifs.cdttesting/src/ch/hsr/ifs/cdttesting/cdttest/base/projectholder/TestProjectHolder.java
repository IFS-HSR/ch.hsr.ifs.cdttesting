package ch.hsr.ifs.cdttesting.cdttest.base.projectholder;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ch.hsr.ifs.iltis.core.data.StringInputStream;
import ch.hsr.ifs.iltis.core.exception.ILTISException;
import ch.hsr.ifs.iltis.core.resources.FileUtil;

import ch.hsr.ifs.cdttesting.cdttest.formatting.FormatterLoader;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


public class TestProjectHolder extends AbstractProjectHolder implements ITestProjectHolder {

   private static final String DEFAULT_INDEXER_TIMEOUT_SEC = "10";
   private static final String INDEXER_TIMEOUT_PROPERTY    = "indexer.timeout";
   protected static final int  INDEXER_TIMEOUT_SEC         = Integer.parseInt(System.getProperty(INDEXER_TIMEOUT_PROPERTY,
         DEFAULT_INDEXER_TIMEOUT_SEC));

   private boolean isExpectedProject;

   private List<ICProject> referencedProjects = new ArrayList<>();

   private ArrayList<IPath>                        stagedExternalIncudePaths  = new ArrayList<>();
   private ArrayList<IPath>                        stagedInternalIncludePaths = new ArrayList<>();
   private ArrayList<ReferencedProjectDescription> stagedReferncedProjects    = new ArrayList<>();
   private HashMap<String, String>                 stagedTestSourcesToImport  = new HashMap<>();

   private List<IPath> formattedDocuments;

   public TestProjectHolder(String projectName, Language language, boolean isExpectedProject) {
      this.projectName = projectName;
      this.language = language;
      this.isExpectedProject = isExpectedProject;
   }

   @Override
   public void cleanupProjects() {
      fileCache.clean();
      //TODO(Tobias Stauber) cleanup
      //      try {
      //         fileManager.closeAllFiles();
      //      } catch (CoreException | InterruptedException e) {
      //         e.printStackTrace();
      //      }
      deleteProject(cProject);
      referencedProjects.stream().forEach(this::deleteProject);
   }

   private void deleteProject(ICProject project) {
      try {
         project.getProject().delete(true, true, new NullProgressMonitor());
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   @Override
   public ProjectHolderJob setupIndexAsync() {
      return ProjectHolderJob.create("Setup index of project " + projectName, ITestProjectHolder.SETUP_INDEX_JOB_FAMILY, mon -> {
         setupIndex();
      });
   }

   private void setupIndex() throws CoreException {
      disposeCDTAstCache();
      getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
      ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      /* reindexing will happen automatically after call of setIndexerId */
      CCorePlugin.getIndexManager().setIndexerId(getCProject(), IPDOMManager.ID_FAST_INDEXER);
      referencedProjects.forEach(proj -> CCorePlugin.getIndexManager().setIndexerId(proj, IPDOMManager.ID_FAST_INDEXER));
      ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

      boolean joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor());
      if (!joined) {
         System.err.println("Join on indexer failed. " + projectName + "might fail.");
         joined = CCorePlugin.getIndexManager().joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor());
         if (!joined) {
            System.err.println("Second join on indexer failed.");
         }
      }
      try {
         Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
         assertTrue(CCorePlugin.getIndexManager().joinIndexer(INDEXER_TIMEOUT_SEC * 1000, new NullProgressMonitor())); //CCoreInternals.getPDOMManager()
      } catch (final InterruptedException e) {
         System.err.println("Wait for indexer has been interrupted.");
      }
   }

   @SuppressWarnings("restriction")
   private void disposeCDTAstCache() {
      CUIPlugin.getDefault().getASTProvider().dispose();
   }

   @Override
   public ITestProjectHolder stageAbsoluteExternalIncludePaths(final IPath... absolutePaths) {
      stagedExternalIncudePaths.addAll(Arrays.asList(absolutePaths));
      return this;
   }

   @Override
   public ITestProjectHolder stageInternalIncludePaths(final IPath... projectRelativePaths) {
      stagedInternalIncludePaths.addAll(Arrays.asList(projectRelativePaths));
      return this;
   }

   @Override
   public ITestProjectHolder setLanguage(Language language) {
      this.language = language;
      return this;
   }

   @Override
   public ProjectHolderJob setupProjectReferencesAsync() {
      return ProjectHolderJob.create("Setup members of project " + projectName, ITestProjectHolder.SETUP_PROJECT_REFERENCES_JOB_FAMILY, mon -> {
         if (!referencedProjects.isEmpty()) {
            final ICProjectDescription description = CCorePlugin.getDefault().getProjectDescription(getProject(), true);
            for (final ICConfigurationDescription config : description.getConfigurations()) {
               final Map<String, String> refMap = config.getReferenceInfo();
               for (final ICProject refProject : referencedProjects) {
                  refMap.put(refProject.getProject().getName(), "");
               }
               config.setReferenceInfo(refMap);
            }
            CCorePlugin.getDefault().setProjectDescription(getProject(), description);
         }
      });
   }

   @Override
   public ProjectHolderJob setupIncludePathsAsync() {
      return ProjectHolderJob.create("Adding include path dir to project " + projectName, ITestProjectHolder.ADD_INCLUDE_PATH_JOB_FAMILY, mon -> {
         final int referencedProjectsOffset = stagedExternalIncudePaths.size() + stagedInternalIncludePaths.size();
         final IPath[] pathsToAdd = new IPath[referencedProjectsOffset + referencedProjects.size()];
         int i = 0;
         /* Adds all the external include paths to the array */
         for (; i < stagedExternalIncudePaths.size(); i++) {
            final IPath externalAbsolutePath = stagedExternalIncudePaths.get(i);
            final File folder = externalAbsolutePath.toFile();
            if (!folder.exists()) {
               System.err.println("Adding external include path dir " + externalAbsolutePath + " to test " + projectName + " which does not exist.");
            }
            pathsToAdd[i] = externalAbsolutePath;
         }
         /* Adds all the internal include paths to the array */
         for (int j = 0; j < stagedInternalIncludePaths.size(); i++, j++) {
            final IPath inProjectAbsolutePath = makeProjectAbsolutePath(stagedInternalIncludePaths.get(j));
            final File folder = inProjectAbsolutePath.toFile();
            if (!folder.exists()) {
               System.err.println("Adding external include path dir " + inProjectAbsolutePath.toString() + " to test " + projectName +
                                  " which does not exist.");
            }
            pathsToAdd[i] = inProjectAbsolutePath;
         }
         /* Adds all the referenced project include paths to the array */
         for (int j = 0; j < referencedProjects.size(); i++, j++) {
            final ICProject referencedProj = referencedProjects.get(j);
            pathsToAdd[i] = referencedProj.getProject().getLocation();
         }
         stagedExternalIncudePaths.clear();
         stagedInternalIncludePaths.clear();
         addIncludeRefs(pathsToAdd, referencedProjectsOffset);
         //TODO(Tobias Stauber) figure out why this is needed...
         //         TestScannerProvider.sIncludes = Arrays.stream(pathsToAdd).map(IPath::toOSString).toArray(String[]::new);
      });
   }

   private void addIncludeRefs(final IPath[] pathsToAdd, final int indexOfFirstReferencedProject) {
      try {
         final IPathEntry[] allPathEntries = getCProject().getRawPathEntries();
         final IPathEntry[] newPathEntries = new IPathEntry[allPathEntries.length + pathsToAdd.length];
         System.arraycopy(allPathEntries, 0, newPathEntries, 0, allPathEntries.length);
         int i = 0;
         for (; i < indexOfFirstReferencedProject; i++) {
            newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null, null, pathsToAdd[i], true);
         }
         for (int j = 0; i < pathsToAdd.length; i++, j++) {
            newPathEntries[allPathEntries.length + i] = CoreModel.newIncludeEntry(null, pathsToAdd[j], null, false);
         }
         getCProject().setRawPathEntries(newPathEntries, new NullProgressMonitor());
      } catch (final CModelException e) {
         e.printStackTrace();
      }
   }

   @Override
   public ProjectHolderJob formatFileAsync(IPath path) {
      return ProjectHolderJob.create("Formatting project " + projectName, ITestProjectHolder.FORMATT_FILE_JOB_FAMILY, mon -> {
         if (!formattedDocuments.contains(path)) {
            final IDocument doc = getDocument(getFile(path));
            final Map<String, Object> options = new HashMap<>(cProject.getOptions(true));
            try {
               final ITranslationUnit tu = CoreModelUtil.findTranslationUnitForLocation(path, cProject);
               options.put(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT, tu);
               final CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
               final TextEdit te = formatter.format(CodeFormatter.K_TRANSLATION_UNIT, path.toOSString(), 0, doc.getLength(), 0, NL);
               te.apply(doc);
               formattedDocuments.add(path);
            } catch (CModelException | MalformedTreeException | BadLocationException e) {
               e.printStackTrace();
            }
         }
      });
   }

   @Override
   public ProjectHolderJob loadFormatterAsync() {
      return ProjectHolderJob.create("Loading formatter for project " + projectName, ITestProjectHolder.LOAD_FORMATTER_JOB_FAMILY, mon -> {
         FormatterLoader.loadFormatter(cProject);
      });
   }

   @Override
   public void importFiles() {
      for (URL url : stagedFilesToImport) {
         IFile iFile = getProject().getFile(url.getPath());
         try {
            importFile(iFile, getProject(), url.openStream());
         } catch (IOException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      }

      for (Entry<String, String> entry : stagedTestSourcesToImport.entrySet()) {
         IFile iFile = getProject().getFile(entry.getKey());
         importFile(iFile, getProject(), new StringInputStream(entry.getValue()));
      }
   }

   @Override
   public void stageTestSourceFilesForImport(Collection<TestSourceFile> files) {
      for (TestSourceFile file : files) {
         stagedTestSourcesToImport.put(file.getName(), isExpectedProject ? file.getExpectedSource() : file.getSource());
      }
   }

   @Override
   public ProjectHolderJob setupReferencedProjectsAsync() {
      return ProjectHolderJob.create("Adding referenced project to project " + projectName, ITestProjectHolder.ADD_REFERENCED_PROJ_JOB_FAMILY,
            mon -> setupReferencedProjects());
   }

   private void setupReferencedProjects() throws CoreException {
      for (ReferencedProjectDescription pd : stagedReferncedProjects) {

         final ICProject referencedCProject = createNewProject(pd.getProjectName(), pd.getLanguage());
         for (TestSourceFile file : pd.getSourceFiles()) {
            IProject referencedProject = referencedCProject.getProject();
            importFile(referencedProject.getFile(file.getName()), referencedProject, new StringInputStream(isExpectedProject ? file
                  .getExpectedSource() : file.getSource()));
         }
         this.referencedProjects.add(referencedCProject);
      }
   }

   @Override
   public void stageReferencedProjects(ReferencedProjectDescription... referencedProjects) {
      for (ReferencedProjectDescription pd : referencedProjects) {
         stagedReferncedProjects.add(pd);
      }
   }

   protected void importFile(final IFile file, final IContainer root, final InputStream stream) {
      FileUtil.createFolderWithParents(file, root);
      if (file.exists()) {
         System.err.println("Overwriting existing file which should not yet exist: " + file.getName());
         try {
            file.setContents(stream, true, false, new NullProgressMonitor());
         } catch (CoreException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      } else {
         try {
            if (!file.exists()) {
               file.create(stream, true, new NullProgressMonitor());
            }
         } catch (CoreException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      }
      //TODO(Tobias Stauber) clean after testing
      //      fileManager.addFile(file);
   }

   /* -- Public Getters -- */

   @Override
   public List<ICProject> getReferencedProjects() {
      return referencedProjects;
   }

   @Override
   public ICProject getCProject() {
      return cProject;
   }

   @Override
   public Optional<ICElement> getCElement(IPath path) {
      try {
         return Optional.ofNullable(getCProject().findElement(path));
      } catch (CModelException ignored) {
         return Optional.empty();
      }
   }

   @Override
   public Optional<ICElement> getCElement(IFile file) {
      return getCElement(file.getLocation());
   }

}
