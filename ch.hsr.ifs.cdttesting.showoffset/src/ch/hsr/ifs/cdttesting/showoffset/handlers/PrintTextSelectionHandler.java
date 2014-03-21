package ch.hsr.ifs.cdttesting.showoffset.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import ch.hsr.ifs.cdttesting.showoffset.ShowOffset;

public class PrintTextSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		new ShowOffset().run();
		return null;
	}
}
