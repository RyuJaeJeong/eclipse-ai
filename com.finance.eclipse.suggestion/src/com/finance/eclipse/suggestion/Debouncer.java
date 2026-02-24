package com.finance.eclipse.suggestion;

import java.time.Duration;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Display;

public final class Debouncer {
	
	// Field
	private final Display display;
	private final Supplier<Duration> delaySupplier;
	private Runnable pending;
	
	// Cons
	public Debouncer(Display display, Supplier<Duration> delaySupplier) {
		if(display == null) {
			throw new IllegalArgumentException("Display is null");
		}
		this.display = display;
		this.delaySupplier = delaySupplier;
	}
	
	// Method
	public void debounce(Runnable runnable) {
		if (this.pending != null) {
			this.display.timerExec(-1, this.pending); 		
		}
		
		this.pending = () -> {
			this.pending = null; 
			runnable.run();
		};
		
		this.display.timerExec((int) this.delaySupplier.get().toMillis(), this.pending);
	}
}
