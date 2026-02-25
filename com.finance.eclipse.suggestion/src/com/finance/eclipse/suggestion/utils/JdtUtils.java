package com.finance.eclipse.suggestion.utils;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import com.finance.eclipse.suggestion.AiActivator;

public class JdtUtils {
	
	private JdtUtils() {
		
	}

	public static ICompilationUnit[] getCompilationUnits(final IPackageFragment packageFragment) {
		try {
			return packageFragment.getCompilationUnits();
		} catch (final JavaModelException exception) {
			AiActivator.log().error("Failed to get compilation units for package fragment: " + packageFragment, exception);
			return new ICompilationUnit[0];
		}
	}
	
}
