package com.finance.eclipse.suggestion;

import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

public enum AiImageKey {
	PACKAGE_ICON,
	TYPE_ICON,
	ENUM_ICON,
	INTERFACE_ICON,
	RECORD_ICON,
	ANNOTATION_ICON,
	RESOURCE_ICON,
	SCOPE_ICON,
	COPY_ICON,
	BEFORE_ICON,
	AFTER_ICON,
	PIN_ICON,
	IMPORT_ICON,
	BLACKLIST_ICON,
	FILL_IN_MIDDLE_ICON,
	INFORMATIONS_ICON,
	DEPENDENCIES_ICON,
	EDITOR_ICON,
	ACCEPT_ICON,
	REJECT_ICON,
	RUN_ICON,
	DIFF_NEW_ICON,
	DIFF_OLD_ICON,
	DIFF_LINE_ICON,
	DIFF_CHAR_ICON;

	static void initializeImages(ImageRegistry registry) {
		try {
			registerImage(registry, AiImageKey.PACKAGE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/package_obj.svg").toURL()));
			registerImage(registry, AiImageKey.TYPE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/class_obj.svg").toURL()));
			registerImage(registry, AiImageKey.ENUM_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/enum_obj.svg").toURL()));
			registerImage(registry, AiImageKey.INTERFACE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/innerinterface_public_obj.svg").toURL()));
			registerImage(registry, AiImageKey.RECORD_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/record_obj.svg").toURL()));
			registerImage(registry, AiImageKey.ANNOTATION_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/annotation_obj.svg").toURL()));
			registerImage(registry, AiImageKey.RESOURCE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui.navigator.resources/icons/full/obj16/otherprojects_workingsets.svg").toURL()));
			registerImage(registry, AiImageKey.SCOPE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/elcl16/javaassist_co.svg").toURL()));
			registerImage(registry, AiImageKey.COPY_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/copy_edit.svg").toURL()));
			registerImage(registry, AiImageKey.BEFORE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_l_edit.svg").toURL()));
			registerImage(registry, AiImageKey.AFTER_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/etool16/shift_r_edit.svg").toURL()));
			registerImage(registry, AiImageKey.PIN_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/ovr16/pinned_ovr@2x.svg").toURL()));
			registerImage(registry, AiImageKey.IMPORT_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/import_wiz.svg").toURL()));
			registerImage(registry, AiImageKey.BLACKLIST_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/elcl16/trash.svg").toURL()));
			registerImage(registry, AiImageKey.FILL_IN_MIDDLE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui.workbench.texteditor/icons/full/elcl16/insert_template.svg").toURL()));
			registerImage(registry, AiImageKey.INFORMATIONS_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/obj16/info_tsk.svg").toURL()));
			registerImage(registry, AiImageKey.DEPENDENCIES_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.ui/icons/full/obj16/library_obj.svg").toURL()));
			registerImage(registry, AiImageKey.EDITOR_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.ui/icons/full/etool16/editor_area.svg").toURL()));
			registerImage(registry, AiImageKey.ACCEPT_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.junit/icons/full/obj16/testok.svg").toURL()));
			registerImage(registry, AiImageKey.REJECT_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.jdt.junit/icons/full/obj16/testerr.svg").toURL()));
			registerImage(registry, AiImageKey.RUN_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.debug.ui/icons/full/etool16/run_exc.svg").toURL()));
			registerImage(registry, AiImageKey.DIFF_NEW_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.compare/icons/full/elcl16/copycont_l_co.svg").toURL()));
			registerImage(registry, AiImageKey.DIFF_OLD_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.compare/icons/full/elcl16/copycont_r_co.svg").toURL()));
			registerImage(registry, AiImageKey.DIFF_LINE_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.compare/icons/full/elcl16/syncpane_co.svg").toURL()));
			registerImage(registry, AiImageKey.DIFF_CHAR_ICON, ImageDescriptor.createFromURL(URI.create("platform:/plugin/org.eclipse.compare/icons/full/etool16/ignorews_edit.svg").toURL()));
		} catch (final MalformedURLException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static void registerImage(ImageRegistry registry, AiImageKey imageKey, ImageDescriptor descriptor) {
		registry.put(imageKey.name(), descriptor);
	}
}
