package com.finance.eclipse.suggestion.model;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.finance.eclipse.suggestion.model.history.AiHistoryEntry;

public class Suggestion {
	
	// field
	private final AiHistoryEntry historyEntry;
	private final String content;
	private final int modelOffset;
	private final int originalLength;
	private final int widgetLastLine;
	private final int newLines;
	private final int oldLines;
	private final int additionalLines;
	
	
	// cons
	public Suggestion(AiHistoryEntry historyEntry, String content, int modelOffset, int originalLength, int widgetLastLine, int newLines, int oldLines, int additionalLines) {
		this.historyEntry = historyEntry;
		this.content = content;
		this.modelOffset = modelOffset;
		this.originalLength = originalLength;
		this.widgetLastLine = widgetLastLine;
		this.newLines = newLines;
		this.oldLines = oldLines;
		this.additionalLines = additionalLines;
	}
	
	// method
	public AiHistoryEntry historyEntry(){ 
		return historyEntry; 
	}

	public String content(){ 
		return content; 
	}

	public int modelOffset(){ 
		return modelOffset; 
	}

	public int originalLength(){ 
		return originalLength; 
	}

	public int widgetLastLine(){ 
		return widgetLastLine; 
	}

	public int newLines(){ 
		return newLines; 
	}

	public int oldLines(){ 
		return oldLines; 
	}

	public int additionalLines(){ 
		return additionalLines; 
	}


	public void applyTo(final IDocument document) throws BadLocationException {
		final int offset = this.modelOffset();
		final int length = this.originalLength();
		document.replace(offset, length, this.content());
	}
	
}
