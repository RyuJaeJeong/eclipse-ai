package com.finance.eclipse.suggestion.utils;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;

public class EclipseUtils {
	
	public static String getSelectionText(ITextViewer textViewer) {
		return Display.getDefault().syncCall(() -> {
	        ISelection selection = textViewer.getSelectionProvider().getSelection();
	        return selection instanceof ITextSelection ? ((ITextSelection) selection).getText() : "";
	    });
	}
	
	public static int getWidgetOffset(ITextViewer textViewer, int modelOffset) {
		if (textViewer instanceof final ITextViewerExtension5 extension5) {
			return extension5.modelOffset2WidgetOffset(modelOffset);
		} else {
			return modelOffset;
		}
	}

}
