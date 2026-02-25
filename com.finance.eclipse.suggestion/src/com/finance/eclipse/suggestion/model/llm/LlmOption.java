package com.finance.eclipse.suggestion.model.llm;

public class LlmOption {
	
	// field 
	private final LlmProvider provider;
	private final String modelKey;
	
	// cons 
	public LlmOption(LlmProvider provider, String modelKey) {
		this.provider = provider;
		this.modelKey = modelKey;
	}

	// method
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
		return new LlmOption(LlmProvider.OPENAI, "gpt-5-mini-2025-08-07");
	}

	public static LlmOption createEditModelOptionFromPreferences() {
		return new LlmOption(LlmProvider.OPENAI, "gpt-5-mini-2025-08-07");
	}

	public static LlmOption createGenerateModelOptionFromPreferences() {
		return new LlmOption(LlmProvider.OPENAI, "gpt-5-mini-2025-08-07");
	}

	public static LlmOption createQuickFixModelOptionFromPreferences() {
		return new LlmOption(LlmProvider.OPENAI, "gpt-5-mini-2025-08-07");
	}

	public static LlmOption createNextEditModelOptionFromPreferences() {
		return new LlmOption(LlmProvider.OPENAI, "gpt-5-mini-2025-08-07");
	}
}
