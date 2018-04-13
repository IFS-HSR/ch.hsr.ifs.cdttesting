package ch.hsr.ifs.cdttesting.cdttest.base.projectholder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import ch.hsr.ifs.iltis.core.resources.FileUtil;

import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;


public class ExternalTestResourceProjectHolder extends AbstractProjectHolder implements IProjectHolder {

   public static final String NL = System.getProperty("line.separator");

   protected IWorkspaceRoot workspaceRoot;

   public ExternalTestResourceProjectHolder(String projectName, Language language) {
      this.projectName = projectName;
      this.language = language;
   }

   @Override
   public void cleanupProjects() {
      fileCache.clean();
      deleteProject(cProject.getProject());
   }

   private void deleteProject(IProject project) {
      try {
         project.getProject().delete(true, true, new NullProgressMonitor());
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void importFiles() {
      for (URL res : stagedFilesToImport) {
         createIResourceAndInitializeIt(res, cProject.getProject());
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

}
