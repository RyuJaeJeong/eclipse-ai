package com.finance.eclipse.suggestion.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class Utils {
	
	public static String getStacktraceString(final Throwable throwable) {
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		return stringWriter.toString();
	}
	
	public static boolean checkType(IType type) {
		if (type == null) {
			return false;
		}
		try {
			type.getFlags();
			return true;
		} catch (final JavaModelException exception) {
			return false;
		}
	}
	
	public static String getTypeKeywordLabel(IType type) {
		final int flags = getFlags(type);
		if (Flags.isInterface(flags)) {
			return "interface";
		} else if (Flags.isEnum(flags)) {
			return "enum";
		} else if (Flags.isAnnotation(flags)) {
			return "@interface";
		} else if (Flags.isRecord(flags)) {
			return "record";
		} else {
			return "class";
		}
	}
	
	private static int getFlags(IType type) {
		try {
			return type.getFlags();
		} catch (final JavaModelException exception) {
			throw new RuntimeException("Failed to get type flags for " + type, exception);
		}
	}
	
}
