package com.finance.eclipse.tdd.utils;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

public class JdtUtils {
	
	private JdtUtils() {
		
	}

	public static ICompilationUnit[] getCompilationUnits(final IPackageFragment packageFragment) {
		LoggingUtils logger = new LoggingUtils();
		try {
			return packageFragment.getCompilationUnits();
		} catch (final JavaModelException exception) {
			logger.error("Failed to get compilation units for package fragment: " + packageFragment);
			exception.printStackTrace();
			return new ICompilationUnit[0];
		}
	}
	
}
