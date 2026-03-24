package com.finance.eclipse.security.handlers;

import jakarta.inject.Named;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Shell;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.config.UserPreferences;


public class SecurityAnalysisHandler {
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell s) {		
		System.out.println("보안 진단 시작...");
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		Job job = new Job("취약점 스캔중") {			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("전체 분석", projects.length);
				for(IProject project: projects) {
					if(monitor.isCanceled()) return Status.CANCEL_STATUS;
					try {
						if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                            IJavaProject javaProject = JavaCore.create(project);
                            monitor.setTaskName(project.getName() + " 분석 중...");
                            System.out.println(project.getName() + "분석 중...");
                            runSpotBugs(javaProject, monitor);
                        }
					}catch (Exception e) {
						e.printStackTrace();
					}
					monitor.worked(1);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
	private void runSpotBugs(IJavaProject javaProject, IProgressMonitor progressMonitor) throws JavaModelException, IOException, InterruptedException, URISyntaxException, PluginException{
		Project project = new Project();
		String outputDir = javaProject.getProject().getLocation().append(javaProject.getOutputLocation().removeFirstSegments(1)).toOSString();
		project.addFile(outputDir.replace("\\", "/"));
		System.out.println("[분석 대상 경로]: " + outputDir.replace("\\", "/"));
		BugReporter reporter = new AbstractBugReporter() {
	        @Override
	        public void doReportBug(BugInstance bugInstance) {
	            project.getProjectName();
	        	System.out.println("[발견된 취약점]");
	            System.out.println("타입: " + bugInstance.getType());
	            System.out.println("우선순위: " + bugInstance.getPriorityString());
	            System.out.println("카테고리: " + bugInstance.getCategoryAbbrev());
	            System.out.println("메시지: " + bugInstance.getBugPattern().getShortDescription());
	            
	            if (bugInstance.getPrimarySourceLineAnnotation() != null) {
	                System.out.println("위치: " + bugInstance.getPrimarySourceLineAnnotation().getSourcePath() + ":" + bugInstance.getPrimarySourceLineAnnotation().getStartLine());
	            }	            
	        }

	        @Override
	        public void finish() {
	            System.out.println(">>> [" + javaProject.getProject().getName() + "] 분석 완료");
	        }

	        @Override
	        public edu.umd.cs.findbugs.ProjectStats getProjectStats() {
	            return new edu.umd.cs.findbugs.ProjectStats();
	        }

			@Override
			public BugCollection getBugCollection() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void observeClass(ClassDescriptor arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void reportAnalysisError(AnalysisError arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void reportMissingClass(String arg0) {
				// TODO Auto-generated method stub
				
			}
	    };
	    reporter.setPriorityThreshold(Priorities.NORMAL_PRIORITY);
	    URL corePluginUrl = FindBugs2.class.getProtectionDomain().getCodeSource().getLocation();
	    File coreJar = new File(corePluginUrl.toURI());
	    Plugin.loadCustomPlugin(coreJar, null);
	    DetectorFactoryCollection dfc = DetectorFactoryCollection.instance();
	    FindBugs2 engine = new FindBugs2();
	    engine.setProject(project);
	    engine.setBugReporter(reporter);
	    engine.setDetectorFactoryCollection(dfc);
	    engine.setUserPreferences(UserPreferences.createDefaultUserPreferences());	    
	    engine.execute();
	}


}
