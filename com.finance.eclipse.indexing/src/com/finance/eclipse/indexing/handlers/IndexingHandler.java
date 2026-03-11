package com.finance.eclipse.indexing.handlers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import com.finance.eclipse.indexing.utils.UniqueIDUtil;

import mjson.Json;

public class IndexingHandler extends AbstractHandler {
	
	private static final String URL = "http://127.0.0.1:8000/workspace";
	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job("Source Code Indexing") {
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
		        IWorkspaceRoot root = workspace.getRoot();
		        IProject[] projects = root.getProjects();
		        try {
		        	for (IProject project : projects) {
		        		String uniqueId = UniqueIDUtil.getUniqueId();
		                if(project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
		                	IJavaProject javaProject = JavaCore.create(project);
		                	String projectNm = project.getName();
		                	IPackageFragment[] packages = javaProject.getPackageFragments();
		                	for (IPackageFragment pkg : packages) {
		                		List<Json> methodList = new ArrayList<>();
		                		if(pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
		    						for(ICompilationUnit unit : pkg.getCompilationUnits()) {
		    							IType[] allTypes = unit.getAllTypes();
		    							String filePath = unit.getResource().getProjectRelativePath().toString();
		    							String fileNm = unit.getElementName();
		    							for(IType type: allTypes){
		    								IMethod[] methods = type.getMethods();
		    								String structNm = type.getElementName();
		    								for (IMethod method : methods) {
		    									String signature = getSignature(method);
		    									String symbol = getSymbol(method);
		    									Json contextData = Json.object()
		    														   .set("file_path", filePath)
		    														   .set("file_name", fileNm)
		    														   .set("struct_name", structNm)
		    														   .set("snippet", method.getSource());
		    									Json methodData = Json.object()
		    														  .set("unique_id", UniqueIDUtil.getUniqueId())
		    														  .set("project_nm", projectNm)
		    														  .set("symbol", symbol)
		    														  .set("name", method.getElementName())
		    														  .set("signature", signature)
		    														  .set("code_type", ((Flags.isAbstract(method.getFlags()))?"AbstractMethod":"Method"))
		    														  .set("docstring", "someText")
		    														  .set("context", contextData);
		    									methodList.add(methodData);
		    								}
		    							}
		    						}
		                		}
		                		
		                		if(methodList.size() > 0) {
		                			Json packageData = Json.object().set("unique_id", uniqueId).set("project_nm", projectNm).set("methods", Json.array(methodList.toArray()));
		                			try {
		                				HttpResponse<String> res = fetchData(packageData);
		                				System.out.println("## res.body: " + res.body());
		                			}catch (Exception e) {
										// TODO: handle exception
		                				e.printStackTrace();
		                			}
		                		}
		       
							}
		                }		                    	
			            
			        }
		        	return Status.OK_STATUS;
				} catch (Exception e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
			
		};		
		job.setUser(true);  
		job.schedule();
		
		return null;
	}
	
	public String getSymbol(IMethod method) throws JavaModelException {
	    IType declaringType = method.getDeclaringType();
	    String packageName = declaringType.getPackageFragment().getElementName().replace('.', '/');
	    String className = declaringType.getElementName();
	    String methodName = method.getElementName();
	    String[] paramTypes = method.getParameterTypes();
	    StringBuilder params = new StringBuilder();
	    for (String pt : paramTypes) {
	        params.append(Signature.toString(pt).replace('.', '/'));
	    }

	    return String.format("java/maven/%s/%s#%s(%s).", packageName, className, methodName, params.toString());
	}
	
	/**
	 * Method 정보를 통해 선업무를 뽑아냅니다.
	 * @param method 메서드 정보
	 * @return 선언부 문자열
	 * @throws JavaModelException
	 */
	public String getSignature(IMethod method) throws JavaModelException {
		StringBuilder annotations = new StringBuilder();
		IAnnotation[] iAnnotations = method.getAnnotations();
		for (IAnnotation anno : iAnnotations) {
		    annotations.append("@").append(anno.getElementName()).append("\n");
		}

		// 2. 접근 제어자 및 모디파이어 (public, protected 등)
		String modifiers = Flags.toString(method.getFlags());

		// 3. 리턴 타입 변환 (QString; -> String)
		String returnTypeRaw = method.getReturnType();
		String returnType = Signature.toString(returnTypeRaw);

		// 4. 파라미터 조립
		String[] paramTypes = method.getParameterTypes();
		String[] paramNames = method.getParameterNames(); // 변수명 (builder 등)
		StringBuilder params = new StringBuilder();
		for (int i = 0; i < paramTypes.length; i++) {
		    params.append(Signature.toString(paramTypes[i])) // 타입명
		          .append(" ")
		          .append(paramNames[i]);                    // 변수명
		    if (i < paramTypes.length - 1) params.append(", ");
		}

		// 5. 최종 완성
		String fullSignature = String.format("%s%s %s %s(%s)", 
		    annotations.toString(), 
		    modifiers, 
		    returnType, 
		    method.getElementName(), 
		    params.toString());
		return fullSignature;
	}
	
	/**
	 * HTTP 통신 
	 * @param data 데이터
	 * @return HTTP response
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static HttpResponse<String> fetchData(Json data) throws IOException, InterruptedException{
		HttpRequest request = HttpRequest.newBuilder()
										 .version(HttpClient.Version.HTTP_1_1)
				   						 .uri(URI.create(IndexingHandler.URL))
				   						 .header("Content-Type", "application/json; charset=UTF-8") // 인코딩 명시
			   						 	 .POST(BodyPublishers.ofString(data.toString(), StandardCharsets.UTF_8))
		   						 	 	 .build();
		HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		return response;
	}

}
