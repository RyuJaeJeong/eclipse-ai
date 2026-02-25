package com.finance.eclipse.suggestion;

import java.util.Optional;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.finance.eclipse.suggestion.controller.InlineCompletionController;

public class AiStartup implements IStartup {

	@Override
	public void earlyStartup() {
		AiActivator.log().info("Hello, world!");
		IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow window : workbenchWindows) {
			IWorkbenchPage activePage = window.getActivePage();
			activePage.addPartListener(new IPartListener2() {
				@Override
				public void partActivated(IWorkbenchPartReference partRef) {
					AiActivator.log().info("active page Opened!");
					getTextEditor(partRef).ifPresent(InlineCompletionController::setup);
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference partRef) {
					
				}
			});
			IEditorReference[] editorReferences = activePage.getEditorReferences();
			for (IEditorReference editorReference : editorReferences) {
				IEditorPart editor = editorReference.getEditor(false);
				if(editor instanceof ITextEditor) {
					AiActivator.log().info("ITextEditor Opened!");
					ITextEditor textEditor = (ITextEditor) editor;
					InlineCompletionController.setup(textEditor);
				}
			}
		}
	}
	
	/**
	 * IWorkBenchPartReference to TextEditor
	 */
	public static Optional<ITextEditor> getTextEditor(IWorkbenchPartReference partReference){
		if(partReference instanceof IEditorReference) {
			IEditorReference editorReference = (IEditorReference) partReference;
			IEditorPart editor = editorReference.getEditor(false);
			if(editor instanceof ITextEditor){
				ITextEditor textEditor = (ITextEditor) editor;
				return Optional.of(textEditor);
			}
		}
		return Optional.empty();
	}

}
