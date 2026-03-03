package com.finance.eclipse.indexing;

import org.eclipse.ui.IStartup;

import com.finance.eclipse.indexing.handlers.IndexingHandler;

public class IndexingStartup implements IStartup {

	@Override
	public void earlyStartup() {
		IndexingActivator.log().info("Indexing started from earlyStartup");
		IndexingHandler handler = new IndexingHandler();
		try {
			handler.execute(null);
		}catch (Exception e) {
			IndexingActivator.log().error("Failed to execute indexing handler at startup", e);
		}
	}

}
