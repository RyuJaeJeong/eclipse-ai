package com.finance.eclipse.suggestion.controller;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.texteditor.ITextEditor;

import com.finance.eclipse.suggestion.AiActivator;
import com.finance.eclipse.suggestion.Debouncer;
import com.finance.eclipse.suggestion.model.InlineCompletion;
import com.finance.eclipse.suggestion.context.ContextContext;
import com.finance.eclipse.suggestion.context.ContextEntry;
import com.finance.eclipse.suggestion.context.FillInMiddleContextEntry;
import com.finance.eclipse.suggestion.context.RootContextEntry;
import com.finance.eclipse.suggestion.history.AiHistoryEntry;
import com.finance.eclipse.suggestion.history.HistoryStatus;
import com.finance.eclipse.suggestion.llm.LlmResponse;
import com.finance.eclipse.suggestion.llm.LlmUtils;
import com.finance.eclipse.suggestion.utils.AiCodeCleanupUtils;
import com.finance.eclipse.suggestion.utils.EclipseUtils;
import com.finance.eclipse.suggestion.utils.LambdaExceptionUtils.Runnable_WithExceptions;
import com.finance.eclipse.suggestion.utils.Utils;

public final class InlineCompletionController {
	
	// field
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
	private final CaretListener caretListener;
	private InlineCompletion completion;
	private IContextActivation context;
	private Job job;
	private long changeCounter;
	private long lastChangeCounter;
	private final Debouncer debouncer;
	private boolean abortDisabled;
	private Future<LlmResponse> llmResponseFuture;
	private boolean isDrawing;
	
	// cons 
	private InlineCompletionController(ITextViewer textViewer, ITextEditor textEditor){
		this.textViewer = textViewer;
		this.textEditor = textEditor;
		this.widget = textViewer.getTextWidget();
		this.spacingProvider = new StyledTextLineSpacingProviderImplementation();
		this.documentListener = new DocumentListenerImplementation();
		this.paintListener = new PaintListenerImplementation();
		this.painter = new PainterImplementation();
		this.caretListener = new CaretListenerImplementation();
		this.completion = null;
		this.context = null;
		this.job = null;
		this.changeCounter = 0;
		this.lastChangeCounter = 0;
		this.debouncer = new Debouncer(Display.getDefault(), () -> Duration.ofMillis(400));
		this.abortDisabled = false;
		this.llmResponseFuture = null;
		this.isDrawing = false;
	}
	
	
	// method	
	public static InlineCompletionController setup(ITextEditor textEditor) {
		final ITextViewer textViewer = EclipseUtils.getTextViewer(textEditor);
		return CONTROLLER_BY_VIEWER.computeIfAbsent(textViewer, ignore -> {
			final InlineCompletionController controller = new InlineCompletionController(textViewer, textEditor);
			Display.getDefault().syncExec(() -> {
				((ITextViewerExtension2) textViewer).addPainter(controller.painter);
				textViewer.getTextWidget().addPaintListener(controller.paintListener);
				textViewer.getTextWidget().setLineSpacingProvider(controller.spacingProvider);
				textViewer.getTextWidget().addCaretListener(controller.caretListener);
				textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).addDocumentListener(controller.documentListener);
			});
			return controller;
		});
	}
	
	private void triggerAutocomplete() {
		final boolean isDocumentChanged = this.lastChangeCounter != this.changeCounter;
		this.lastChangeCounter = this.changeCounter;
//		if (!AiCoderPreferences.isAutocompleteEnabled()) {
//			return;
//		}

		if (!isDocumentChanged) {
			return;
		}

		final boolean isActiveEditor = Display.getDefault().syncCall(() -> {
			final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			return activePage != null && activePage.getActiveEditor() == InlineCompletionController.this.textEditor;
		});
		
		if (!isActiveEditor) {
			AiActivator.log().info("Not active editor");
			return;
		}
		
		this.debouncer.debounce(() -> {
			if (!EclipseUtils.hasSelection(this.textViewer)) {
				trigger(null);
			}
		});
	}
	
	/**
	 * 동작을 위한 trigger 함수 
	 * @param instruction 지시문
	 */
	public void trigger(String instruction){
		final long startTime = System.currentTimeMillis();
		abort("Trigger");
		final StyledText widget = InlineCompletionController.this.textViewer.getTextWidget();
		final int lineHeight = widget.getLineHeight();
		final int defaultLineSpacing = widget.getLineSpacing();
		final IEditorInput editorInput = this.textEditor.getEditorInput();
		final String filePath = editorInput.getName();
		final boolean hasSelection = EclipseUtils.hasSelection(this.textViewer);		
		final AiHistoryEntry historyEntry = new AiHistoryEntry(filePath, this.textViewer.getDocument().get());
		this.job = new Job("AI completion") {
			ITextViewer textViewer = InlineCompletionController.this.textViewer;
			ITextEditor textEditor = InlineCompletionController.this.textEditor;
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String prompt = "";
				LlmResponse llmResponse = null;
				try {
					final int modelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
					final IDocument document = this.textViewer.getDocument();
					if (monitor.isCanceled()) {
						historyEntry.setStatus(HistoryStatus.CANCELED);
						AiActivator.log().warn(historyEntry.toString());
						return Status.CANCEL_STATUS;
					}
					
					AiActivator.log().info("Calculate context");
					final RootContextEntry rootContextEntry = RootContextEntry.create(document, this.textEditor.getEditorInput(), modelOffset);
					final String contextString = ContextEntry.apply(rootContextEntry, new ContextContext());
					final String[] contextParts = contextString.split(FillInMiddleContextEntry.FILL_HERE_PLACEHOLDER);
					final String prefix = contextParts[0];
					final String suffix = contextParts.length > 1 ? contextParts[1] : "";
					prompt = prefix + "<|cursor|>" + suffix;
					InlineCompletionController.this.llmResponseFuture = LlmUtils.executeFillInTheMiddle(prefix, suffix);					
					AiActivator.log().info("Wait for LLM response");
					try {
						llmResponse = InlineCompletionController.this.llmResponseFuture.get();
					}catch(Exception exception) {
						if(exception.getCause() instanceof CancellationException) {
							return Status.CANCEL_STATUS;
						}
						throw exception;
					}															
					if (llmResponse.isError()) {
						historyEntry.setStatus(HistoryStatus.ERROR);
						historyEntry.setPlainLlmResponse(llmResponse.getPlainResponse());
						historyEntry.setModelLabel(llmResponse.getLlmModelOption().getLabel());
						historyEntry.setInput(prompt);
						historyEntry.setOutput(llmResponse.getContent());
						AiActivator.log().error(historyEntry.toString());
						return Status.OK_STATUS;
					}					
					final String content = Utils.stripCodeMarkdownTags(llmResponse.getContent());
					final int currentModelOffset = EclipseUtils.getCurrentOffsetInDocument(InlineCompletionController.this.textEditor);
					final boolean isMultilineContent = content.contains("\n");
					final boolean isBlank = content.isBlank();
					final boolean isMoved = currentModelOffset != modelOffset;
					final boolean isSame = hasSelection? false:isMultilineContent && suffix.replaceAll("\\s", "").startsWith(content.replaceAll("\\s", ""));
					if (!isBlank && !isMoved && !isSame) {
						setup(InlineCompletion.create(
								historyEntry,
								document,
								modelOffset,
								EclipseUtils.getWidgetOffset(this.textViewer, modelOffset),
								EclipseUtils.getWidgetLine(this.textViewer, modelOffset),
								content,
								lineHeight,
								defaultLineSpacing));
					}
					final long duration = System.currentTimeMillis() - startTime;
					historyEntry.setStatus(calculateStatus(isBlank, isMoved, isSame));
					historyEntry.setDurationMs(duration);
					historyEntry.setLlmDurationMs(llmResponse.getDuration().toMillis());
					historyEntry.setPlainLlmResponse(llmResponse.getPlainResponse());
					historyEntry.setModelLabel(llmResponse.getLlmModelOption().getLabel());
					historyEntry.setInputTokenCount(llmResponse.getInputTokens());
					historyEntry.setOutputTokenCount(llmResponse.getOutputTokens());
					historyEntry.setInput(prompt);
					historyEntry.setOutput(content);
					AiActivator.log().info(historyEntry.toString());
					return Status.OK_STATUS;
				} catch (Exception e) {
					AiActivator.log().error("AI Coder completion failed", e);
					final long duration = System.currentTimeMillis() - startTime;
					final String stacktrace = Utils.getStacktraceString(e);
					historyEntry.setStatus(HistoryStatus.ERROR);
					historyEntry.setDurationMs(duration);
					historyEntry.setLlmDurationMs(0);
					historyEntry.setPlainLlmResponse(llmResponse != null ? llmResponse.getPlainResponse() : "");
					historyEntry.setModelLabel(null);
					historyEntry.setInputTokenCount(0);
					historyEntry.setOutputTokenCount(0);
					historyEntry.setInput(prompt);
					historyEntry.setOutput((llmResponse != null ? llmResponse.getContent() : "") + stacktrace);
					AiActivator.log().error(historyEntry.toString());
					return Status.OK_STATUS;
				}
				
			}

			@Override
			protected void canceling() {
				AiActivator.log().info("AI Coder completion Job Canceled!!!");
				cancelHttpRequest();
			}			
		};
		this.job.setRule(COMPLETION_JOB_RULE);
		this.job.schedule();
		
	}
	
	private void cancelHttpRequest() {
		AiActivator.log().info("Canceling");
		if (this.llmResponseFuture != null) {
			AiActivator.log().info("Cancel LLM response future");
			this.llmResponseFuture.cancel(true);
			this.llmResponseFuture = null;
		}
	}
	
	private HistoryStatus calculateStatus(boolean isBlank, boolean isMoved, boolean isSame) {
		if (isMoved) {
			return HistoryStatus.MOVED;
		} else if (isBlank) {
			return HistoryStatus.BLANK;
		} else if (isSame) {
			return HistoryStatus.EQUAL;
		} else {
			return HistoryStatus.GENERATED;
		}
	}
	
	private void setup(InlineCompletion completion) {
		AiActivator.log().info("Activate context (completion)");
		this.completion = completion;
		setupContext();
		redraw();
	}
	
	private void setupContext() {
		Display.getDefault().syncExec(() -> {
			this.context = EclipseUtils.getContextService(this.textEditor).activateContext("com.finance.eclipse.suggestion.inlineCompletionVisible");
		});
	}
	
	private void redraw() {
		Display.getDefault().syncExec(() -> {
			this.textViewer.getTextWidget().redraw();
		});
	}
	
	private synchronized <T extends Exception> void executeThenAbort(Runnable_WithExceptions<T> runnable, String reason) throws T {
		try {
			this.abortDisabled = true;
			runnable.run();
		} finally {
			this.abortDisabled = false;
			abort(reason);
		}
	}
	
	/**
	 * field 초기화
	 * @param reason 취소 이유
	 */
	public void abort(String reason){
		if(this.abortDisabled) {
			return;
		}
		
		if (this.llmResponseFuture != null) {
			AiActivator.log().info(String.format("Cancel LLM response future (reason: '%s')", reason));
			this.llmResponseFuture.cancel(true);
			this.llmResponseFuture = null;
		}
		
		
		if (this.job != null) {
			AiActivator.log().info(String.format("Abort job (reason: '%s')", reason));
			this.job.cancel();
			this.job = null;
		}
		
		if (this.context != null) {
			AiActivator.log().info(String.format("Deactivate context (reason: '%s')", reason));
			EclipseUtils.getContextService(this.textEditor).deactivateContext(this.context);
			this.context = null;
		}
		
		if (this.completion != null) {
			AiActivator.log().info(String.format("Unset completions (reason: '%s')", reason));
			if (this.completion.historyEntry().getStatus() == HistoryStatus.GENERATED) {
				this.completion.historyEntry().setStatus(HistoryStatus.REJECTED);
			}
			this.completion = null;
			this.paintListener.resetMetrics();
		}
	}
	
	public void accept() {
		if (this.completion != null) {
			acceptInlineCompletion();
		}
		
		if (true) {
			AiActivator.log().info("Trigger code cleanup on apply");
			final Optional<ICompilationUnit> compilationUnitOptional = EclipseUtils.getCompilationUnit(this.textEditor.getEditorInput());
			if (compilationUnitOptional.isPresent()) {
				final ICompilationUnit compilationUnit = compilationUnitOptional.get();
				try {
					AiCodeCleanupUtils.triggerSaveActions(compilationUnit);
				} catch (OperationCanceledException | CoreException exception) {
					AiActivator.log().error("Failed to cleanup code", exception);
				}
			}
		}
	}

	private void acceptInlineCompletion() {
		try {
			executeThenAbort(() -> { 
				this.completion.applyTo(this.textViewer.getDocument());
				this.textViewer.setSelectedRange(this.completion.modelRegion().getOffset() + this.completion.content().length(), 0);
				this.completion.historyEntry().setStatus(HistoryStatus.ACCEPTED);
				this.completion.historyEntry().setContent(this.textViewer.getDocument().get());
			}, "Accepted");
		} catch (final BadLocationException exception) {
			throw new RuntimeException("Failed to accept inline completion", exception);
		}
	}
	
	// implementation
	private class CaretListenerImplementation implements CaretListener {
		@Override
		public void caretMoved(CaretEvent event) {
			System.out.println("caret Moved!!!!!!!!!!!!!!!!!!!");
			abort("Caret moved!");
			triggerAutocomplete();
		}
	}
	
	private class DocumentListenerImplementation implements IDocumentListener {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			InlineCompletionController.this.changeCounter++;
			abort("Document changed");
		}
	}	
	
	
	private class StyledTextLineSpacingProviderImplementation implements StyledTextLineSpacingProvider {
		@Override
		public Integer getLineSpacing(int lineIndex) {
			final InlineCompletion completion = InlineCompletionController.this.completion;
			if (completion != null && completion.widgetLineIndex() == lineIndex) {
				return completion.lineSpacing();
			}
			return null;
		}
	}
	
	private class PaintListenerImplementation implements PaintListener {
		
		private final Set<GlyphMetrics> modifiedMetrics;
		
		public PaintListenerImplementation() {
			this.modifiedMetrics = new HashSet<>();
		}
		
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
