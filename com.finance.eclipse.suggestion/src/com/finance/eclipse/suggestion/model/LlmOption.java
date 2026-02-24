package com.finance.eclipse.suggestion.model;

public class LlmOption {
	
	// field 
	private final LlmProvider provider;
	private final String modelKey;
	
	// cons 
	public LlmOption(LlmProvider provider, String modelKey) {
		this.provider = provider;
		this.modelKey = modelKey;
	}

	// 기존 record 방식과의 호환성을 위한 Getter
	public LlmProvider provider() {
		return provider;
	}

	public String modelKey() {
		return modelKey;
	}

	public String getLabel() {
		return this.provider.name() + " - " + this.modelKey;
	}

	public static LlmOption createFillInMiddleModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getFillInMiddleProvider(), AiCoderPreferences.getFillInMiddleModel());
	}

	public static LlmOption createEditModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getEditProvider(), AiCoderPreferences.getEditModel());
	}

	public static LlmOption createGenerateModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getGenerateProvider(), AiCoderPreferences.getGenerateModel());
	}

	public static LlmOption createQuickFixModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getQuickFixProvider(), AiCoderPreferences.getQuickFixModel());
	}

	public static LlmOption createNextEditModelOptionFromPreferences() {
		return new LlmOption(AiCoderPreferences.getNextEditProvider(), AiCoderPreferences.getNextEditModel());
	}
}
