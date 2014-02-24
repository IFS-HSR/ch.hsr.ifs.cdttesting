package ch.hsr.ifs.cdttesting.example.examplerefactoringtest;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

@SuppressWarnings("restriction")
public class TestRefactoring extends CRefactoring {


	private boolean collectModificationsCalled;
	private boolean checkInitialConditionsCalled;
	private boolean checkFinalConditionsCalled;

	public TestRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return null;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus result = super.checkInitialConditions(pm);
		checkInitialConditionsCalled = true;
		return result;
	}

	@Override
	protected void collectModifications(IProgressMonitor pm, ModificationCollector collector) throws CoreException, OperationCanceledException {
		IASTTranslationUnit ast = refactoringContext.getAST(tu, pm);
		collectModificationsCalled = ast != null;
	}

	@Override
	protected RefactoringStatus checkFinalConditions(IProgressMonitor subProgressMonitor, CheckConditionsContext checkContext) throws CoreException, OperationCanceledException {
		RefactoringStatus result = super.checkFinalConditions(subProgressMonitor, checkContext);
		checkFinalConditionsCalled = true;
		return result;
	}

	public boolean wasRefactoringSuccessful() {
		return checkInitialConditionsCalled && checkFinalConditionsCalled && collectModificationsCalled;
	}
}