package ch.hsr.ifs.cdttesting.showoffset;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SelectionDialog extends Dialog {
	private final InputHandler handler;
	private int from;
	private int to;

	public SelectionDialog(Shell parent, InputHandler handler) {
		super(parent);
		this.handler = handler;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		FillLayout thisLayout = new FillLayout(SWT.HORIZONTAL);
		thisLayout.marginWidth = 10;
		thisLayout.marginHeight = 10;
		thisLayout.spacing = 5;
		composite.setLayout(thisLayout);
		Label fromLabel = new Label(composite, SWT.NONE);
		fromLabel.setText("&From:");
		final Text fromText = new Text(composite, SWT.BORDER);
		fromText.setSize(50, 50);
		Label toLabel = new Label(composite, SWT.NONE);
		toLabel.setText("&To:");
		final Text toText = new Text(composite, SWT.BORDER);
		toText.setSize(50, 50);

		fromText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				from = Integer.parseInt(fromText.getText());
			}
		});

		toText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				to = Integer.parseInt(toText.getText());
			}
		});
		return composite;
	}

	@Override
	protected void okPressed() {
		super.okPressed();
		handler.setInput(from, to);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Input Selection");
	}

}
