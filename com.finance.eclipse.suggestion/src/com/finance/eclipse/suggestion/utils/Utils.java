package com.finance.eclipse.suggestion.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
	
	public static String getStacktraceString(final Throwable throwable) {
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		return stringWriter.toString();
	}
}
