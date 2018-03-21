package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.eclipse.cdt.core.testplugin.CProjectHelper;
import org.eclipse.cdt.core.testplugin.FileManager;
import org.eclipse.cdt.core.testplugin.TestScannerProvider;
import org.eclipse.cdt.core.testplugin.util.BaseTestCase;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ch.hsr.ifs.iltis.core.data.StringInputStream;
import ch.hsr.ifs.iltis.core.exception.ILTISException;
import ch.hsr.ifs.iltis.core.resources.FileUtil;

import ch.hsr.ifs.cdttesting.cdttest.formatting.FormatterLoader;
import ch.hsr.ifs.cdttesting.helpers.FileCache;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


public class TestProjectHolder implements ITestProjectHolder {

   public static final String NL = System.getProperty("line.separator");

   protected boolean   instantiateCPPProject = true;
   protected FileCache fileCache             = new FileCache();

   protected ICProject  cProject;
   protected IWorkspace workspace;
   protected String     projectName;
   private boolean      isExpectedProject;

   protected FileManager fileManager;

   //TODO use faster collections
   private List<ICProject> referencedProjects = new ArrayList<>();

   private ArrayList<IPath>                         stagedExternalIncudePaths  = new ArrayList<>();
   private ArrayList<IPath>                         stagedInternalIncludePaths = new ArrayList<>();
   private LinkedList<URL>                          stagedFilesToImport        = new LinkedList<>();
   private LinkedList<ReferencedProjectDescription> stagedReferncedProjects    = new LinkedList<>();
   private HashMap<String, String>                  stagedTestSourcesToImport  = new HashMap<>();

   private List<IPath> formattedDocuments;

   public TestProjectHolder(String projectName, boolean isExpectedProject) {
      this.projectName = projectName;
      this.isExpectedProject = isExpectedProject;
   }

   @Override
   public ProjectHolderJob createProjectAsync() {
      return ProjectHolderJob.create("Initializing project " + projectName, IProjectHolder.CREATE_PROJ_JOB_FAMILY, mon -> {
         createProject();
      });
   }

   @Override
   public void createProject() {
      if (CCorePlugin.getDefault() != null && CCorePlugin.getDefault().getCoreModel() != null) {
         workspace = ResourcesPlugin.getWorkspace();
         try {
            if (instantiateCPPProject) {
               cProject = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ 
            } else {
               cProject = CProjectHelper.createCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ 
            }
         } catch (final CoreException ignored) {}
         if (cProject == null) {
            fail("Unable to create project"); //$NON-NLS-1$
         }
         fileManager = new FileManager();
      }
   }

   @Override
   public ProjectHolderJob cleanupProjectsAsync() {
      return ProjectHolderJob.create("Cleaningup project " + projectName, IProjectHolder.CLEANUP_PROJ_JOB_FAMILY, mon -> {
         cleanupProjects();
      });
   }

   @Override
   public void cleanupProjects() {
      fileCache.clean();
      try {
         fileManager.closeAllFiles();
      } catch (CoreException | InterruptedException e) {
         e.printStackTrace();
      }
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
         BaseTestCase.waitForIndexer(getCProject());
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
   public ITestProjectHolder instantiateCProject() {
      this.instantiateCPPProject = false;
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
            /* impl from addIncludeRefs(), was moved here, for calculating it twice is bs */
            //            final IPath referencedProjPath = referencedProjects.get(j).getPath().makeRelative();
            pathsToAdd[i] = referencedProj.getProject().getLocation();
         }
         stagedExternalIncudePaths.clear();
         stagedInternalIncludePaths.clear();
         addIncludeRefs(pathsToAdd, referencedProjectsOffset);
         TestScannerProvider.sIncludes = Arrays.stream(pathsToAdd).map(IPath::toOSString).toArray(String[]::new);
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
   public ProjectHolderJob importFilesAsync() {
      return ProjectHolderJob.create("Importing files into project " + projectName, IProjectHolder.IMPORT_FILES_JOB_FAMILY, mon -> {

         try {
            importFiles();
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      });
   }

   @Override
   public void importFiles() {
      while (!stagedFilesToImport.isEmpty()) {
         URL url = stagedFilesToImport.pop();
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
   public void stageFilesForImport(Collection<URI> files) {
      for (URI uri : files) {
         try {
            stagedFilesToImport.add(uri.toURL());
         } catch (MalformedURLException e1) {
            e1.printStackTrace();
         }
      }
   }

   @Override
   public void stageFilesForImport(Enumeration<URL> files) {
      while (files.hasMoreElements()) {
         stagedFilesToImport.add(files.nextElement());
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
         final ICProject referencedCProject = CProjectHelper.createCCProject(pd.getProjectName(), "bin", IPDOMManager.ID_NO_INDEXER);
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
            file.create(stream, true, new NullProgressMonitor());
         } catch (CoreException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      }
      fileManager.addFile(file);
   }

   /* -- Public Getters -- */

   @Override
   public IPath makeProjectAbsolutePath(final String relativePath) {
      return getProject().getLocation().append(relativePath);
   }

   @Override
   public IPath makeProjectAbsolutePath(final IPath relativePath) {
      return getProject().getLocation().append(relativePath);
   }

   @Override
   public URI makeProjectAbsoluteURI(final String relativePath) {
      return URI.create(makeProjectAbsolutePath(relativePath).toOSString());
   }

   @Override
   public URI makeProjectAbsoluteURI(final IPath relativePath) {
      return URI.create(makeProjectAbsolutePath(relativePath).toOSString());
   }

   @Override
   public List<ICProject> getReferencedProjects() {
      return referencedProjects;
   }

   @Override
   public IFile getFile(String filePath) {
      return getProject().getFile(filePath);
   }

   @Override
   public IFile getFile(IPath filePath) {
      return getProject().getFile(filePath);
   }

   @Override
   public IDocument getDocument(URI sourceFile) {
      return fileCache.getDocument(sourceFile);
   }

   @Override
   public IDocument getDocument(IFile sourceFile) {
      return fileCache.getDocument(sourceFile);
   }

   @Override
   public IDocument getDocumentFromRelativePath(String relativePath) {
      return getDocument(getFile(relativePath));
   }

   @Override
   public IDocument getDocumentFromRelativePath(IPath projectRelativePath) {
      return getDocument(getFile(projectRelativePath));
   }

   @Override
   public IProject getProject() {
      return getCProject().getProject();
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
