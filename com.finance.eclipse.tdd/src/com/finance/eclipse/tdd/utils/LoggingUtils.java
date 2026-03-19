package com.finance.eclipse.tdd.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public class LoggingUtils {
	
	public void info(String msg) {
		Platform.getLog(FrameworkUtil.getBundle(getClass())).log(new Status(IStatus.INFO, "com.finance.eclipse.tdd", msg));
	}
	
	public void error(String msg) {
		Platform.getLog(FrameworkUtil.getBundle(getClass())).log(new Status(IStatus.ERROR, "com.finance.eclipse.tdd", msg));
	}
}
