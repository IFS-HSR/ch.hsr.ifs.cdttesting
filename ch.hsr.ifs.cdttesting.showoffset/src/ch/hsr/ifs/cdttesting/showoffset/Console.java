package ch.hsr.ifs.cdttesting.showoffset;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Console {

	private static final String name = "ShowOffset";

	private static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = plugin.getConsoleManager();
		IConsole[] existing = consoleManager.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		return createNewConsole(name, consoleManager);
	}

	private static MessageConsole createNewConsole(String name, IConsoleManager conMan) {
		MessageConsole newConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { newConsole });
		return newConsole;
	}

	public static void print(String text) {
		revealConsole();
		MessageConsole myConsole = findConsole(name);
		MessageConsoleStream out = myConsole.newMessageStream();
		out.println(text);
	}

	private static void revealConsole() {
		IConsole myConsole = findConsole(name);
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		IConsoleView view;
		try {
			view = (IConsoleView) page.showView(id);
			view.display(myConsole);
		} catch (PartInitException e) {
			return;
		}
	}

}
