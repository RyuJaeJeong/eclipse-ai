package com.finance.eclipse.suggestion.utils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.finance.eclipse.suggestion.AiActivator;

public final class GitUtils {
	
	private GitUtils() {
		
	}

	public static GitState getGitState(IProject project) throws IOException {
		final Repository repository = getRepositoryFromProject(project);
		final Set<String> ignoredPaths = getGitIgnoredPaths(project);
		return new GitState(repository, ignoredPaths);
	}

	private static Set<String> getGitIgnoredPaths(IProject project) {
		try {
			final Repository repository = GitUtils.getRepositoryFromProject(project);
			if (repository == null) {
				AiActivator.getDefault().getLog().info("No git repository found for project " + project.getName());
				return Set.of();
			}
			try (Git git = new Git(repository)) {
				return git.status().call().getIgnoredNotInIndex();
			}
		} catch (final Exception exception) {
			AiActivator.getDefault().getLog().error("Error getting git ignored paths", exception);
			return Set.of();
		}
	}

	private static Repository getRepositoryFromProject(IProject project) throws IOException {
		if (project == null || !project.exists()) {
			return null;
		}
		final IPath projectLocation = project.getLocation();
		if (projectLocation == null) {
			return null;
		}
		final File projectDir = projectLocation.toFile();
		final FileRepositoryBuilder builder = new FileRepositoryBuilder();
		final File gitDir = findGitDir(projectDir);
		if (gitDir == null) {
			return null;
		}
		return builder
				.setGitDir(findGitDir(projectDir))
				.readEnvironment() 
				.findGitDir(projectDir) 
				.build();
	}

	private static File findGitDir(File projectDir) {
		File currentDir = projectDir;
		while (currentDir != null) {
			final File gitDir = new File(currentDir, ".git");
			if (gitDir.exists() && gitDir.isDirectory()) {
				return gitDir;
			}
			currentDir = currentDir.getParentFile();
		}
		return null;
	}

	public static class GitState {
		private final Repository repository;
		private final Set<String> ignoredPaths;

		public GitState(Repository repository, Set<String> ignoredPaths) {
			this.repository = repository;
			this.ignoredPaths = ignoredPaths;
		}

		public boolean isIgnored(IResource resource) {
			if (this.repository == null) {
				return false;
			}
			return this.ignoredPaths.contains(this.repository.getWorkTree().toPath().relativize(resource.getLocation().toFile().toPath()).toString());
		}
	}
}
