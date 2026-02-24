package com.finance.eclipse.suggestion;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class AiActivator extends AbstractUIPlugin {
	
	// Field
	public static final String PLUGIN_ID = "com.finance.eclipse.suggestion";
	private static AiActivator plugin;
	
	// Cons
	public AiActivator() {
	
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

	public static AiActivator getDefault() {
		return plugin;
	}
	
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		AiImageKey.initializeImages(registry);
	}
	
	public static Image getImage(AiImageKey imageKey){
		return getDefault().getImageRegistry().get(imageKey.name());
	}
	
	public static ILog log() {
		return getDefault().getLog();
	}
	
	public static void openErrorDialog(String title, String message, Throwable throwable) {
		ErrorDialog.openError(null, title, null, new Status(IStatus.ERROR, AiActivator.PLUGIN_ID, message, throwable));
	}


}
