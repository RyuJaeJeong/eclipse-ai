package com.finance.eclipse.indexing.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class UniqueIDUtil {
	
	private final static File idFile = new File(System.getProperty("user.home"), ".colId");
	
	public static String getUniqueId() throws IOException {
		if(UniqueIDUtil.idFile.exists()) {
			return Files.readString(idFile.toPath(), StandardCharsets.UTF_8).trim();
		}else {
			String newId = UUID.randomUUID().toString();
            Files.writeString(idFile.toPath(), newId, StandardCharsets.UTF_8);
            System.out.println("## New IDE ID Created: " + newId);
            return newId;
		}
	}
}
