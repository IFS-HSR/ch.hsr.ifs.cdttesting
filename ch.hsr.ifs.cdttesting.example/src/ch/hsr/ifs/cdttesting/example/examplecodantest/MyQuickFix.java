package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.eclipse.cdt.codan.ui.AbstractAstRewriteQuickFix;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;

public class MyQuickFix extends AbstractAstRewriteQuickFix {

	@Override
	public void modifyAST(IIndex index, IMarker marker) {
		//a real test should use ModificationCollector/ASTRewrite or Refactoring here.
		//For testing puropses, we only replace some text directly.
		TextFileChange change = new TextFileChange("rename foo to bar text edit", (IFile) marker.getResource());
		change.setEdit(new ReplaceEdit(4, 3, "bar"));
		try {
			change.perform(new NullProgressMonitor());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getLabel() {
		return "My QuickFix";
	}

}
