package ch.hsr.ifs.cdttesting.cdttest.base;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;

import ch.hsr.ifs.iltis.core.exception.ILTISException;

import ch.hsr.ifs.cdttesting.helpers.FileCache;
import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;


public abstract class AbstractProjectHolder implements IProjectHolder {

   protected ICProject cProject;
   protected String    projectName;

   protected FileCache fileCache = new FileCache();

   protected LinkedList<URL> stagedFilesToImport = new LinkedList<>();
   protected Language language;
   protected IWorkspace workspace;

   public AbstractProjectHolder() {
      super();
   }

   @Override
   public void stageFilesForImport(Enumeration<URL> resources) {
      while (resources.hasMoreElements()) {
         stagedFilesToImport.add(resources.nextElement());
      }
   }

   @Override
   public void stageFilesForImport(Collection<URI> files) {
      for (URI uri : files) {
         try {
            stagedFilesToImport.add(uri.toURL());
         } catch (MalformedURLException e) {
            ILTISException.wrap(e).rethrowUnchecked();
         }
      }
   }

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
      return cProject.getProject();
   }

   @Override
   public ProjectHolderJob createProjectAsync() {
      return ProjectHolderJob.create("Initializing project " + projectName, IProjectHolder.CREATE_PROJ_JOB_FAMILY, mon -> {
         createProject();
      });
   }

   @Override
   public ProjectHolderJob cleanupProjectsAsync() {
      return ProjectHolderJob.create("Cleaningup project " + projectName, IProjectHolder.CLEANUP_PROJ_JOB_FAMILY, mon -> {
         cleanupProjects();
      });
   }

   @Override
   public ProjectHolderJob importFilesAsync() {
      return ProjectHolderJob.create("Importing files into project " + projectName, IProjectHolder.IMPORT_FILES_JOB_FAMILY, mon -> {
         importFiles();
      });
   }

}