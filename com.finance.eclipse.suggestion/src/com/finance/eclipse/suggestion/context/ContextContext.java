package com.finance.eclipse.suggestion.context;

import java.util.HashSet;
import java.util.Set;

public class ContextContext {
	
	// field
	private final Set<ContextEntryKey> doneKeys;
	
	// cons
	public ContextContext() {
		this.doneKeys = new HashSet<>();
	}
	
	// method
	public boolean isDone(ContextEntry entry) {
		return this.doneKeys.contains(entry.getKey());
	}

	public void markDone(ContextEntry entry) {
		this.doneKeys.add(entry.getKey());
	}
	
}