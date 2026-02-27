package com.finance.eclipse.suggestion.context;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import com.finance.eclipse.suggestion.AiActivator;
import com.finance.eclipse.suggestion.AiImageKey;
import com.finance.eclipse.suggestion.preference.ContextPreferences;
import com.finance.eclipse.suggestion.utils.LambdaExceptionUtils;


public class BlacklistedContextEntry extends ContextEntry {
	public static final String PREFIX = "BLACKLISTED";

	private BlacklistedContextEntry(List<? extends ContextEntry> childContextEntries, Duration creationDuration) {
		super(childContextEntries, creationDuration);
	}

	@Override
	public ContextEntryKey getKey() {
		return new ContextEntryKey(PREFIX, PREFIX);
	}

	@Override
	public String getLabel() {
		return "Blacklist";
	}

	@Override
	public String getContent(ContextContext context) {
		return super.getContent(context) + "\n";
	}

	@Override
	public Image getImage() {
		return AiActivator.getImage(AiImageKey.BLACKLIST_ICON);
	}

	public static BlacklistedContextEntry create() throws CoreException {
		final long before = System.currentTimeMillis();
		final List<? extends ContextEntry> entries = ContextPreferences.getBlacklist().stream()
				.map(LambdaExceptionUtils.rethrowFunction(Context::create))
				.flatMap(Optional::stream)
				.toList();
		return new BlacklistedContextEntry(entries, Duration.ofMillis(System.currentTimeMillis() - before));
	}
}