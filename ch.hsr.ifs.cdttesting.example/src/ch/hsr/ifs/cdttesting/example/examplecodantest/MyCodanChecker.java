package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.eclipse.cdt.codan.core.cxx.model.AbstractIndexAstChecker;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;


public class MyCodanChecker extends AbstractIndexAstChecker {

	/**
	 * Note that this is/must be the same checker-id as defined in plugin.xml
	 */
	public static final String MY_CHECKER_ID = "ch.hsr.ifs.myCodanCheckerId";
	/**
	 * Note that this is/must be the same problem-id as defined in plugin.xml
	 */
	public static final String MY_PROBLEM_ID = "ch.hsr.ifs.myCodanProblemId";

	@Override
	public void processAst(IASTTranslationUnit ast) {
		IASTFunctionDefinition firstDecl = (IASTFunctionDefinition) ast.getDeclarations()[0];
		String name = firstDecl.getDeclarator().getName().getRawSignature();
		reportProblem(MY_PROBLEM_ID, firstDecl, name); // note that the name-string is inserted into the "messagePattern" by replacing the "{0}" of the pattern.
	}
}
