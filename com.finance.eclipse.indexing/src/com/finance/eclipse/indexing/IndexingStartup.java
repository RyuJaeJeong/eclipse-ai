package com.finance.eclipse.indexing;

import org.eclipse.ui.IStartup;

public class IndexingStartup implements IStartup {

	@Override
	public void earlyStartup() {
		IndexingActivator.log().info("Hello, world!");
	}

}
