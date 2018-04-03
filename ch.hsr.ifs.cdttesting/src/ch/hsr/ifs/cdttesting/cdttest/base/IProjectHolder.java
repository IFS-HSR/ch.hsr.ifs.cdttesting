package ch.hsr.ifs.cdttesting.cdttest.base;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;


public interface IProjectHolder {

   public static final String CREATE_PROJ_JOB_FAMILY  = "ch.hsr.ifs.cdttesting.holderjob.createproj_job_family";
   public static final String CLEANUP_PROJ_JOB_FAMILY = "ch.hsr.ifs.cdttesting.holderjob.cleanupproj_job_family";
   public static final String IMPORT_FILES_JOB_FAMILY = "ch.hsr.ifs.cdttesting.holderjob.importfiles_job_family";

   /**
    * This creates and an Eclipse Job which creates the project for this holder.
    * 
    * @return The job.
    */
   public ProjectHolderJob createProjectAsync();

   /**
    * Creates the project for this holder.
    */
   public void createProject();

   /**
    * This creates an Eclipse Job which cleans this holder. This means it deletes the project and all referenced projects held.
    * 
    * @return The job.
    */
   public ProjectHolderJob cleanupProjectsAsync();

   /**
    * Cleans this holder. This deletes its project and all referenced projects on execution.
    */
   public void cleanupProjects();

   /**
    * This add files for import into this holder.
    * 
    * @throws Exception
    */
   public void stageFilesForImport(Collection<URI> files);

   /**
    * This add files for import into this holder.
    */
   public void stageFilesForImport(Enumeration<URL> files);

   /**
    * This creates an Eclipse Job which imports the files added by {@link #stageFilesForImport(Collection)} or
    * {@link #stageFilesForImport(Enumeration)}.
    * 
    * @return The job.
    */
   public ProjectHolderJob importFilesAsync();

   /**
    * Imports the files added by {@link #stageFilesForImport(Collection)} or {@link #stageFilesForImport(Enumeration)}.
    * 
    * @throws Exception
    */
   public void importFiles();

   /**
    * @return The absolute path for the relative path in this holders project
    */
   public IPath makeProjectAbsolutePath(final String relativePath);

   /**
    * @return The absolute path for the relative path in this holders project
    */
   public IPath makeProjectAbsolutePath(final IPath relativePath);

   /**
    * @return The absolute URI for the relative path in this holders project
    */
   public URI makeProjectAbsoluteURI(final String relativePath);

   /**
    * @return The absolute URI for the relative path in this holders project
    */
   public URI makeProjectAbsoluteURI(final IPath relativePath);

   /* -- Public Getters -- */

   /**
    * Returns the {@code IFile} which corresponds to the file path.
    * 
    * @param filePath
    *        The file path
    * @return The IFile for this path. The file is not guaranteed to exist.
    */
   public IFile getFile(String filePath);

   /**
    * Returns the {@code IFile} which corresponds to the file path.
    * 
    * @param filePath
    *        The file path
    * @return The IFile for this path. The file is not guaranteed to exist.
    */
   public IFile getFile(IPath filePath);

   /**
    * Returns the {@code IDocument} which is located behind this URI. This method uses a file cache.
    * 
    * @param sourceFile
    *        The URI of the source file
    * @return The {@code IDocument} or {@code null} if the URI is invalid
    */
   public IDocument getDocument(URI sourceFile);

   /**
    * Returns the {@code IDocument} which is located behind this IFile. This method uses a file cache.
    * 
    * @param sourceFile
    *        The IFile of the source file
    * @return The {@code IDocument} or {@code null} if the IFile does not exist
    */
   public IDocument getDocument(IFile sourceFile);

   /**
    * Returns the {@code IDocument} which is located behind this relative path. This method uses a file cache.
    * 
    * @param relativePath
    *        The relative path to the source file as String
    * @return The {@code IDocument} or {@code null} if the path is invalid
    */
   public IDocument getDocumentFromRelativePath(String relativePath);

   /**
    * Returns the {@code IDocument} which is located behind this IPath. This method uses a file cache.
    * 
    * @param relativePath
    *        The IPath of the source file
    * @return The {@code IDocument} or {@code null} if the IPath does not exist
    */
   public IDocument getDocumentFromRelativePath(IPath relativePath);

   /**
    * @return The {@code IProject} of the {@code ICProject} held by this holder. This is a convenience method for {@code getCProject().getProject()}.
    */
   public IProject getProject();

   /**
    * An Eclipse job which allows to create new jobs with a family
    * 
    * @author tstauber
    *
    */
   public abstract class ProjectHolderJob extends Job {

      private String familyConstant = "NONE";

      public static ProjectHolderJob VOID_JOB = new ProjectHolderJob("Void Job", "NONE") {

         @Override
         public boolean shouldRun() {
            return false;
         }

         @Override
         public boolean shouldSchedule() {
            return false;
         }

         @Override
         protected IStatus run(IProgressMonitor monitor) {
            return null;
         }

      };

      private ProjectHolderJob lastInChain = this;

      protected ProjectHolderJob(String name, String familyConstant) {
         super(name);
         this.familyConstant = familyConstant;
      }

      public void chain(ProjectHolderJob nextJob) {
         this.lastInChain = nextJob.lastInChain;
         this.addJobChangeListener(new JobChangeAdapter() {

            @Override
            public void done(IJobChangeEvent event) {
               nextJob.schedule();
               super.done(event);
            }
         });
      }

      public static ProjectHolderJob create(String name, String familyConstant, IJobFunction function) {
         return new ProjectHolderJob(name, familyConstant) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
               return function.run(monitor);
            }
         };
      }

      public static ProjectHolderJob create(String name, String familyConstant, final ICoreRunnable runnable) {
         return new ProjectHolderJob(name, familyConstant) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
               try {
                  runnable.run(monitor);
               } catch (CoreException e) {
                  IStatus st = e.getStatus();
                  return new Status(st.getSeverity(), st.getPlugin(), st.getCode(), st.getMessage(), e);
               }
               return Status.OK_STATUS;
            }
         };
      }

      /**
       * @return {@code true} iff family is a string, and the family constant used to create this job begins with family.
       */
      @Override
      public boolean belongsTo(Object family) {
         if (family instanceof String) {
            return familyConstant.startsWith((String) family);
         } else {
            return false;
         }
      }

      /**
       * Caution this force executes the runnable in this job.
       * 
       * @return
       */
      public IStatus forceRun() {
         return this.run(new NullProgressMonitor());
      }

      public static ProjectHolderJob merge(ProjectHolderJob thisJob, ProjectHolderJob otherJob) {
         if (thisJob == null && otherJob == null) return null;
         if (thisJob != null && otherJob == null) return thisJob;
         if (thisJob == null && otherJob != null) return otherJob;
         return new ProjectHolderJob(thisJob.getName(), thisJob.familyConstant) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
               IStatus firstStatus = thisJob.run(monitor);
               IStatus secondStatus = otherJob.run(monitor);
               MultiStatus mergedStatus = new MultiStatus(firstStatus.getPlugin(), firstStatus.getCode(), "Merged status for two jobs", null);
               mergedStatus.merge(firstStatus);
               mergedStatus.merge(secondStatus);
               return mergedStatus;
            }
         };
      }

      public ProjectHolderJob getLastInChain() {
         return lastInChain;
      }
   }

}
