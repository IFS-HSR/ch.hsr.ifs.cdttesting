package ch.hsr.ifs.cdttesting.cdttest.base.projectholder;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;

import ch.hsr.ifs.iltis.core.exception.ILTISException;

import ch.hsr.ifs.cdttesting.testsourcefile.RTSTest.Language;
import ch.hsr.ifs.cdttesting.testsourcefile.TestSourceFile;


public class FakeProjectHolder implements ITestProjectHolder {

   public FakeProjectHolder(String projectName) {}

   private ILTISException getNoFunctionalityException() {
      return new ILTISException("This project holder does not provide any functionality!");
   }

   @Override
   public ProjectHolderJob createProjectAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public ProjectHolderJob cleanupProjectsAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public ProjectHolderJob setupIndexAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public FakeProjectHolder stageAbsoluteExternalIncludePaths(final IPath... paths) {
      return this;
   }

   @Override
   public FakeProjectHolder stageInternalIncludePaths(final IPath... projectRelativePaths) {
      return this;
   }

   @Override
   public FakeProjectHolder setLanguage(Language language) {
      return this;
   }

   @Override
   public ProjectHolderJob setupProjectReferencesAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public ProjectHolderJob setupIncludePathsAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public ProjectHolderJob formatFileAsync(IPath path) {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public ProjectHolderJob loadFormatterAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public void stageReferencedProjects(ReferencedProjectDescription... referencedProjects) {}

   protected void importFile(final IFile file, final IContainer root, final String content) throws Exception {}

   /* -- Public Getters -- */

   @Override
   public IPath makeProjectAbsolutePath(final String relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IPath makeProjectAbsolutePath(final IPath relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public URI makeProjectAbsoluteURI(String relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public URI makeProjectAbsoluteURI(IPath relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public List<ICProject> getReferencedProjects() {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IFile getFile(String filePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IFile getFile(IPath filePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IDocument getDocument(URI sourceFile) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IDocument getDocument(IFile sourceFile) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IDocument getDocumentFromRelativePath(String relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IDocument getDocumentFromRelativePath(IPath relativePath) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public IProject getProject() {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public ICProject getCProject() {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public Optional<ICElement> getCElement(IPath path) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public Optional<ICElement> getCElement(IFile file) {
      throw getNoFunctionalityException().rethrowUnchecked();
   }

   @Override
   public ProjectHolderJob importFilesAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

   @Override
   public void createProject() {}

   @Override
   public void cleanupProjects() {}

   @Override
   public void stageFilesForImport(Collection<URI> files) {}

   @Override
   public void stageFilesForImport(Enumeration<URL> files) {}

   @Override
   public void importFiles() {}

   @Override
   public void stageTestSourceFilesForImport(Collection<TestSourceFile> files) {}

   @Override
   public ProjectHolderJob setupReferencedProjectsAsync() {
      return ProjectHolderJob.VOID_JOB;
   }

}
