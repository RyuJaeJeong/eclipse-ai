package com.finance.eclipse.indexing;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class IndexingActivator extends AbstractUIPlugin {
	
	// Field
	public static final String PLUGIN_ID = "com.finance.eclipse.indexing"; 
	private static IndexingActivator plugin;
	
	// Cons
	public IndexingActivator() {
	
	}
	
	// Method
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		log().info("[eclipse-ai] " + PLUGIN_ID + " loaded");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	
	public static IndexingActivator getDefault() {
		return plugin;
	}
	
	public static ILog log(){
		return getDefault().getLog();
	}
	
	public static void openInfoDialog(String title, String message, Throwable throwable) {
		openInfoDialog(title, message, throwable);
	}
	
	public static void openErrorDialog(String title, String message, Throwable throwable) {
		ErrorDialog.openError(null, title, null, new Status(IStatus.ERROR, IndexingActivator.PLUGIN_ID, message, throwable));
	}

}
