package ch.hsr.ifs.cdttesting.cdttest.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

import ch.hsr.ifs.iltis.core.exception.ILTISException;
import ch.hsr.ifs.iltis.core.resources.FileUtil;

import ch.hsr.ifs.cdttesting.helpers.FileCache;


public class ExternalTestResourceProjectHolder implements IProjectHolder {

   public static final String NL = System.getProperty("line.separator");

   protected FileCache fileCache = new FileCache();

   protected IProject        project;
   protected IWorkspaceRoot  workspaceRoot;
   protected String          projectName;
   protected LinkedList<URL> filesToImport = new LinkedList<>();

   public ExternalTestResourceProjectHolder(String projectName) {
      this.projectName = projectName;
   }

   @Override
   public ProjectHolderJob createProjectAsync() {
      return ProjectHolderJob.create("Initializing project " + projectName, IProjectHolder.CREATE_PROJ_JOB_FAMILY, mon -> {
         createProject();
      });
   }

   @Override
   public void createProject() {
      workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      try {
         project = workspaceRoot.getProject(projectName);
         if (!project.exists()) {
            project.create(new NullProgressMonitor());
         }
         if (!project.isOpen()) {
            project.open(new NullProgressMonitor());
         }
      } catch (CoreException e) {
         ILTISException.wrap(e).rethrowUnchecked();
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
      deleteProject(project);
   }

   private void deleteProject(IProject project) {
      try {
         project.getProject().delete(true, true, new NullProgressMonitor());
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   @Override
   public ProjectHolderJob importFilesAsync() {
      return ProjectHolderJob.create("Importing files into project " + projectName, IProjectHolder.IMPORT_FILES_JOB_FAMILY, mon -> {
         importFiles();
      });
   }

   @Override
   public void importFiles() {
      while (!filesToImport.isEmpty()) {
         createIResourceAndInitializeIt(filesToImport.pop(), project);
      }
   }

   @Override
   public void stageFilesForImport(Enumeration<URL> resources) {
      while (resources.hasMoreElements()) {
         filesToImport.add(resources.nextElement());
      }
   }

   @Override
   public void stageFilesForImport(Collection<URI> files) {
      for (URI uri : files) {
         try {
            filesToImport.add(uri.toURL());
         } catch (MalformedURLException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      }
   }

   private void createIResourceAndInitializeIt(URL sourceURL, IContainer root) {
      IPath path = new Path(sourceURL.getPath()).removeFirstSegments(1);
      if (isFolderURL(sourceURL)) {
         IFolder folder = root.getFolder(path);
         FileUtil.createFolderWithParents(folder, root);
      } else {
         IFile file = root.getFile(path);
         if (!file.exists()) {
            FileUtil.createFolderWithParents(file, root);
            try (InputStream istream = sourceURL.openStream()) {
               file.create(istream, true, new NullProgressMonitor());
            } catch (CoreException | IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   private static boolean isFolderURL(URL url) {
      return url.getPath().endsWith(String.valueOf(File.separatorChar));
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
      return project;
   }

}
