package ch.hsr.ifs.cdttesting.cdttest.base.projectholder;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import ch.hsr.ifs.iltis.core.data.AbstractPair;

import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


public interface ITestProjectHolder extends IProjectHolder {

   public static final String SETUP_INDEX_JOB_FAMILY              = "ch.hsr.ifs.cdttesting.holderjob.setupindex_job_family";
   public static final String SETUP_PROJECT_REFERENCES_JOB_FAMILY = "ch.hsr.ifs.cdttesting.holderjob.setupprojref_job_family";
   public static final String ADD_INCLUDE_PATH_JOB_FAMILY         = "ch.hsr.ifs.cdttesting.holderjob.addincludepath_job_family";
   public static final String ADD_REFERENCED_PROJ_JOB_FAMILY      = "ch.hsr.ifs.cdttesting.holderjob.addreferencedproj_job_family";
   public static final String LOAD_FORMATTER_JOB_FAMILY           = "ch.hsr.ifs.cdttesting.holderjob.loadformatter_job_family";
   public static final String FORMATT_FILE_JOB_FAMILY             = "ch.hsr.ifs.cdttesting.holderjob.formattfile_job_family";
   String                     NL                                  = System.getProperty("line.separator");

   /**
    * This creates an Eclipse Job which sets up the index for the project held.
    * 
    * @return The job.
    */
   public ProjectHolderJob setupIndexAsync();

   /**
    * Adds another external include directory. All calls to this methods must take place before the Job returned by
    * {@code createAddIncludePathDirJob()} is executed.
    * 
    * @param path
    *        The path to add
    * @return Itself for chaining
    */
   public ITestProjectHolder stageAbsoluteExternalIncludePaths(final IPath... paths);

   /**
    * Adds another internal include directory. All calls to this methods must take place before the Job returned by
    * {@code createAddIncludePathDirJob()} is executed.
    * 
    * @param path
    *        The path to add
    * @return Itself for chaining
    */
   public ITestProjectHolder stageInternalIncludePaths(final IPath... projectRelativePaths);

   /**
    * Instantiates a project for the passed language. All calls to this methods must take place before the Job returned by
    * {@code createCreateProjectJob()} is executed.
    * 
    * @return Itself for chaining
    */
   public ITestProjectHolder setLanguage(Language lang);

   /**
    * This creates an Eclipse Job which sets up the project references.
    * 
    * @return The job.
    */
   public ProjectHolderJob setupProjectReferencesAsync();

   /**
    * This creates an Eclipse Job which adds the include directories to the project held.
    * 
    * @return The job.
    */
   public ProjectHolderJob setupIncludePathsAsync();

   /**
    * This creates an Eclipse Job which sets the referenced projects up.
    * This Job should be executed before either {@link #setupProjectReferences()} or
    * {@link #setupIncludePaths()} is executed.
    * 
    * @return The job.
    */
   public ProjectHolderJob setupReferencedProjectsAsync();

   /**
    * This creates an Eclipse Job which creates the referenced projects and loads its files. This Job should be executed before one of
    * the
    * jobs created by calling {@code createSetupProjectReferencesJob()} or {@code createAddIncludePathDirJob()} is executed.
    * 
    * @return The job.
    */
   public void stageReferencedProjects(ReferencedProjectDescription... referencedProjects);

   /**
    * This creates an Eclipse Job which formats the file located at the URI.
    * 
    * @return The job.
    */
   public ProjectHolderJob formatFileAsync(IPath path);

   /**
    * This creates an Eclipse Job which loads the CDTTesting formatter into this holder's project.
    * 
    * @return The job.
    */
   public ProjectHolderJob loadFormatterAsync();

   /**
    * This creates an Eclipse Job which imports the files added by {@link #stageFilesForImport(Collection)}, {@link #stageFilesForImport(Enumeration)}
    * or
    * {@link #stageTestSourceFilesForImport(Collection)}.
    * 
    * @return The job.
    */
   @Override
   public ProjectHolderJob importFilesAsync();

   /**
    * Imports the files added by {@link #stageFilesForImport(Collection)}, {@link #stageFilesForImport(Enumeration)} or
    * {@link #stageTestSourceFilesForImport(Collection)}.
    * 
    * @throws Exception
    */
   @Override
   public void importFiles();

   /**
    * This add files for import into this holder.
    */
   public void stageTestSourceFilesForImport(Collection<TestSourceFile> files);

   /* -- Public Getters -- */

   /**
    * @return The {@code ICProject}s referenced by this holder
    */
   public List<ICProject> getReferencedProjects();

   /**
    * @return The {@code ICProject} held by this holder.
    */
   public ICProject getCProject();

   /**
    * @return The {@code ICElement} corresponding to this IPath.
    */
   public Optional<ICElement> getCElement(IPath path);

   /**
    * @return The {@code ICElement} corresponding to this IFile.
    */
   public Optional<ICElement> getCElement(IFile file);

   /**
    * A simple holder for a project name and the files used to populate the project
    */
   public class ReferencedProjectDescription extends AbstractPair<AbstractPair<String, Language>, List<TestSourceFile>> {

      public ReferencedProjectDescription(String projectName, Language lang, List<TestSourceFile> sourceFiles) {
         super(new NestingHelper<>(projectName, lang), sourceFiles);
      }

      public String getProjectName() {
         return ((NestingHelper<String, Language>) first).first();
      }

      public Language getLanguage() {
         return ((NestingHelper<String, Language>) first).second();
      }

      public List<TestSourceFile> getSourceFiles() {
         return second;
      }

   }

}
