package ch.hsr.ifs.cdttesting.junitextensions.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;


@SuppressWarnings("restriction")
public class JumpToMostSpecificClassHandler extends AbstractHandler {

   private String       className;
   private IJavaProject project;

   private Shell shell;

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);

      if (selection instanceof TreeSelection) {
         shell = HandlerUtil.getActiveShell(event);
         getProject(event);
         getTestInfo((TestElement) selection.getFirstElement());
         jump();
      }

      return null;
   }

   private void getProject(ExecutionEvent event) {
      TestRunnerViewPart view = (TestRunnerViewPart) HandlerUtil.getActivePart(event);
      project = view.getLaunchedProject();
   }

   private void getTestInfo(TestElement entry) {
      if (entry instanceof TestCaseElement) {
         className = entry.getClassName();
      } else if (entry instanceof TestSuiteElement) {
         TestSuiteElement testSuite = (TestSuiteElement) entry;
         if (!testSuite.getClassName().contains(".")) {
            className = testSuite.getParent().getClassName();
         } else {
            className = testSuite.getTestName();
         }
      }
   }

   private void jump() {
      if (className != null) {
         try {
            JavaUI.openInEditor(findType(project.getJavaProject(), className));
         } catch (CoreException | NullPointerException e) {
            MessageDialog.openError(shell, "Jump to most specific class", "Failed to find most specific class.");
         }
      }
   }

   protected final IType findType(final IJavaProject project, String className) {
      final IType[] result = { null };
      final String dottedName = className.replace('$', '.');
      try {
         PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
               try {
                  if (project != null) {
                     result[0] = internalFindType(project, dottedName, new HashSet<IJavaProject>(), monitor);
                  }
                  if (result[0] == null) {
                     int lastDot = dottedName.lastIndexOf('.');
                     TypeNameMatchRequestor nameMatchRequestor = new TypeNameMatchRequestor() {

                        @Override
                        public void acceptTypeNameMatch(TypeNameMatch match) {
                           result[0] = match.getType();
                        }
                     };
                     new SearchEngine().searchAllTypeNames(lastDot >= 0 ? dottedName.substring(0, lastDot).toCharArray() : null,
                           SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, (lastDot >= 0 ? dottedName.substring(lastDot + 1)
                                                                                                       : dottedName).toCharArray(),
                           SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, IJavaSearchConstants.TYPE, SearchEngine
                                 .createWorkspaceScope(), nameMatchRequestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);
                  }
               } catch (JavaModelException e) {
                  throw new InvocationTargetException(e);
               }
            }
         });
      } catch (InvocationTargetException e) {
         JUnitPlugin.log(e);
      } catch (InterruptedException e) {
         // user cancelled
      }
      return result[0];
   }

   private IType internalFindType(IJavaProject project, String className, Set<IJavaProject> visitedProjects, IProgressMonitor monitor)
         throws JavaModelException {
      try {
         if (visitedProjects.contains(project)) return null;
         SubMonitor sMon = SubMonitor.convert(monitor);
         monitor.beginTask("", 2); //$NON-NLS-1$
         IType type = project.findType(className, sMon.split(1));
         if (type != null) return type;
         visitedProjects.add(project);
         IJavaModel javaModel = project.getJavaModel();
         String[] requiredProjectNames = project.getRequiredProjectNames();
         IProgressMonitor reqMonitor = sMon.split(1);
         reqMonitor.beginTask("", requiredProjectNames.length); //$NON-NLS-1$
         for (String requiredProjectName : requiredProjectNames) {
            IJavaProject requiredProject = javaModel.getJavaProject(requiredProjectName);
            if (requiredProject.exists()) {
               type = internalFindType(requiredProject, className, visitedProjects, sMon.split(1));
               if (type != null) return type;
            }
         }
         return null;
      } finally {
         monitor.done();
      }
   }

}
