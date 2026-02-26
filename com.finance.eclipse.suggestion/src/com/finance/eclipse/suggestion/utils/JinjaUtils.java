package com.finance.eclipse.suggestion.utils;

import java.util.Map;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;

public class JinjaUtils {
	
	// field
	private static final Jinjava JINJAVA = new Jinjava(JinjavaConfig.newBuilder()
			.withTrimBlocks(true)
			.build());

	
	// cons
	private JinjaUtils() {
	
	}
	
	
	// method
	public static String applyTemplate(String template, Map<String, ?> bindings) {
		return JINJAVA.render(template, bindings);
	}
	
}
