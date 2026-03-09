package com.finance.eclipse.indexing.handlers;

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

import mjson.Json;

public class IndexingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject[] projects = root.getProjects();
        for (IProject project : projects) {
            try {
                if(project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
                	IJavaProject javaProject = JavaCore.create(project);
                	IPackageFragment[] packages = javaProject.getPackageFragments();
                	for (IPackageFragment pkg : packages) {
                		List<Json> packageList = new ArrayList<>();
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
    									Json contextData = Json.object()
    														   .set("file_path", filePath)
    														   .set("file_name", fileNm)
    														   .set("struct_name", structNm)
    														   .set("snippet", method.getSource());
    									Json methodData = Json.object()
    														  .set("name", method.getElementName())
    														  .set("signature", signature)
    														  .set("code_type", ((Flags.isAbstract(method.getFlags()))?"AbstractMethod":"Method"))
    														  .set("docstring", "")
    														  .set("context", contextData);
    									packageList.add(methodData);
    								}
    							}
    						}
                		}
                		
                		if(packageList.size()>0) System.out.println("Package: " + pkg.getElementName());
                		for (int i = 0; i < packageList.size(); i++) {
							System.out.println(packageList.get(i).toString());
						}
                		if(packageList.size()>0) System.out.println("###########################################");
					}
                }
            	
            	
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
		return null;
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

}
