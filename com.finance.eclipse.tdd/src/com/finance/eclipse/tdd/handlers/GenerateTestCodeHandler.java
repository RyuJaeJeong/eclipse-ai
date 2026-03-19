package com.finance.eclipse.tdd.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class GenerateTestCodeHandler{

	@Execute
	public Object execute(Shell shell, IWorkbench workbench){
		MessageDialog.openInformation(shell, "Tdd","Hello, Eclipse world");
		return null;
	}
}
