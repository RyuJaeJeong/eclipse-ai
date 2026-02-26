package com.finance.eclipse.suggestion.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.texteditor.ITextEditor;

import com.finance.eclipse.suggestion.AiActivator;
import com.finance.eclipse.suggestion.controller.InlineCompletionController;
import com.finance.eclipse.suggestion.utils.EclipseUtils;

public class AcceptHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		AiActivator.log().info("Execute accept handler");
		final ITextEditor textEditor = EclipseUtils.getActiveTextEditor().orElseThrow(() -> new ExecutionException("No active text editor"));
		InlineCompletionController.setup(textEditor).accept();
		return null;
	}

}
