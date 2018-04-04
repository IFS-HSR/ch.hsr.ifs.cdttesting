package ch.hsr.ifs.cdttesting.cdttest.base;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.testplugin.CProjectHelper;
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
   public void createProject() {
      if (CCorePlugin.getDefault() != null && CCorePlugin.getDefault().getCoreModel() != null) {
         workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
         try {
            switch (language) {
            case CPP:
               cProject = CProjectHelper.createCCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ 
               break;
            case C:
               cProject = CProjectHelper.createCProject(projectName, "bin", IPDOMManager.ID_NO_INDEXER); //$NON-NLS-1$ 
               break;
            default:
               fail("Invalid language for this holder. Valid choices are: Language.C, Language.CPP ");
            }
         } catch (final CoreException ignored) {
            fail("Failed to create the project");
         }
      }
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
      while (!stagedFilesToImport.isEmpty()) {
         createIResourceAndInitializeIt(stagedFilesToImport.pop(), cProject.getProject());
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
