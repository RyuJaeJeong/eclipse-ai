package com.finance.eclipse.suggestion.controller;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextLineSpacingProvider;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

import com.finance.eclipse.suggestion.Debouncer;
import com.finance.eclipse.suggestion.model.InlineCompletion;
import com.finance.eclipse.suggestion.model.Suggestion;

public final class InlineCompletionController {
	
	// Field
	private static final ISchedulingRule COMPLETION_JOB_RULE = new ISchedulingRule() {
		@Override
		public boolean isConflicting(ISchedulingRule other) {
			return this == other;
		}
		
		@Override
		public boolean contains(ISchedulingRule other) {
			return this == other;
		}
	};
	
	private static final Map<ITextViewer, InlineCompletionController> CONTROLLER_BY_VIEWER;
	static {
		CONTROLLER_BY_VIEWER = new ConcurrentHashMap<>();
	}
	
	private final ITextViewer textViewer;
	private final ITextEditor textEditor;
	private final StyledText widget;
	private final StyledTextLineSpacingProviderImplementation spacingProvider;
	private final DocumentListenerImplementation documentListener;
	private final PaintListenerImplementation paintListener;
	private final PainterImplementation painter;
	private final ISelectionChangedListener selectionListener;
	private final CaretListener caretListener;
	private InlineCompletion completion;
	private IContextActivation context;
	private Job job;
	private long changeCounter;
	private long lastChangeCounter;
	private final Debouncer debouncer;
	private boolean abortDisabled;
	private SuggestionPopupDialog suggestionPopupDialog;
	private Suggestion suggestion;
	
	
	// cons 
	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor){
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.selectionListener = new SelectionListenerImplementation();
		this.caretListener = new CaretListenerImplementation();
		this.completion = null;
		this.context = null;
		this.job = null;
		this.changeCounter = 0;
		this.lastChangeCounter = 0;
		this.debouncer = new Debouncer(Display.getDefault(), () -> Duration.ofMillis(400));
		this.abortDisabled = false;
		this.suggestionPopupDialog = null;
		this.suggestion = null;
	}
	
	
	
	
	
	
	
	
	
	
	private class CaretListenerImplementation implements CaretListener {
		@Override
		public void caretMoved(CaretEvent event) {
			System.out.println("caret Moved!!!!!!!!!!!!!!!!!!!");
			//triggerAutocomplete();
		}
	}
	
	private class DocumentListenerImplementation implements IDocumentListener {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			InlineCompletionController.this.changeCounter++;
//			abort("Document changed");
		}
	}
	
	private class SelectionListenerImplementation implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			final ISelection selection = event.getSelection();
			if (!(selection instanceof ITextSelection)) {
				return;
			}
			final ITextSelection textSelection = (ITextSelection) selection;
			if (textSelection.getLength() <= 0) {
				return;
			}
//			abort("Selection changed");
		}
	}
	
	private class StyledTextLineSpacingProviderImplementation implements StyledTextLineSpacingProvider {
		@Override
		public Integer getLineSpacing(int lineIndex) {
			final InlineCompletion completion = InlineCompletionController.this.completion;
			if (completion != null && completion.widgetLineIndex() == lineIndex) {
				return completion.lineSpacing();
			}

//			final Suggestion suggestion = InlineCompletionController.this.suggestion;
//			final SuggestionPopupDialog suggestionPopupDialog = InlineCompletionController.this.suggestionPopupDialog;
//			if (suggestionPopupDialog != null && suggestion != null && suggestion.widgetLastLine() == lineIndex) {
//				return (suggestionPopupDialog.getLineCount() - suggestion.oldLines() + 2) * InlineCompletionController.this.widget.getLineHeight(); // +2 for the buttons
//			}
			return null;
		}
	}
	
	private class PaintListenerImplementation implements PaintListener {
		
		// field
		private final Set<GlyphMetrics> modifiedMetrics;
		
		// cons
		public PaintListenerImplementation() {
			this.modifiedMetrics = new HashSet<>();
		}
		
		// method
		@Override
		public void paintControl(PaintEvent event) {
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			final Font font = widget.getFont();
			final InlineCompletion completion = InlineCompletionController.this.completion;
			if (completion != null) {
				final Point location = widget.getLocationAtOffset(completion.widgetOffset());
				final List<String> lines = completion.lines();
				event.gc.setBackground(new Color(200, 255, 200));
				event.gc.setForeground(widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				event.gc.setFont(font);
				for (int i = 0; i < lines.size(); i++) {
					final String line = lines.get(i);
					if (i == 0) {
						// first line
						event.gc.drawText(completion.firstLineFillPrefix(), location.x, location.y, true);
						if (completion.firstLineSuffixCharacter() != null) {
							final int suffixCharacterWidth = event.gc.textExtent(completion.firstLineSuffixCharacter()).x;
							final int suffixWidth = event.gc.textExtent(completion.firstLineSuffix()).x;
							final int fillPrefixWidth = event.gc.textExtent(completion.firstLineFillPrefix()).x;
							final StyleRange styleRange = widget.getStyleRangeAtOffset(completion.widgetOffset());
							final int metricWidth = fillPrefixWidth + suffixCharacterWidth;
							if (needMetricUpdate(styleRange, metricWidth)) {
								updateMetrics(event, completion, widget, metricWidth);
							}
							event.gc.drawText(completion.firstLineSuffixCharacter(), location.x + fillPrefixWidth, location.y, false);
							event.gc.drawText(completion.firstLineFillSuffix(), location.x + fillPrefixWidth + suffixWidth, location.y, true);
						}
					} else {
						event.gc.drawText(line.replace("\t", " ".repeat(InlineCompletionController.this.widget.getTabs())), -widget.getHorizontalPixel(), location.y + i * completion.lineHeight(), true);
					}
				}
			}
		}

		private boolean needMetricUpdate(final StyleRange styleRange, final int metricWidth) {
			return styleRange == null || styleRange.metrics == null || styleRange.metrics.width != metricWidth;
		}

		private void updateMetrics(PaintEvent event, InlineCompletion completion, StyledText widget, int metricWidth) {
			final FontMetrics fontMetrics = event.gc.getFontMetrics();
			final StyleRange newStyleRange = new StyleRange(completion.widgetOffset(), 1, null, null);
			newStyleRange.metrics = new GlyphMetrics(fontMetrics.getAscent(), fontMetrics.getDescent(), metricWidth);
			widget.setStyleRange(newStyleRange);
			this.modifiedMetrics.add(newStyleRange.metrics);
			// TODO update style after font size zoom
		}

		public void resetMetrics() {
			final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
			final StyleRange[] styleRanges = widget.getStyleRanges();
			for (final StyleRange styleRange : styleRanges) {
				if (this.modifiedMetrics.contains(styleRange.metrics)) {
					styleRange.metrics = null;
					widget.setStyleRange(styleRange);
				}
			}
			this.modifiedMetrics.clear();
		}
	}
	
	private class PainterImplementation implements IPainter {
		@Override
		public void dispose() {
			CONTROLLER_BY_VIEWER.remove(InlineCompletionController.this.textViewer);
		}

		@Override
		public void paint(int reason) {
		}

		@Override
		public void deactivate(boolean redraw) {
		}

		@Override
		public void setPositionManager(IPaintPositionManager manager) {
			// TODO Auto-generated method stub
			
		}
	}
	
}
