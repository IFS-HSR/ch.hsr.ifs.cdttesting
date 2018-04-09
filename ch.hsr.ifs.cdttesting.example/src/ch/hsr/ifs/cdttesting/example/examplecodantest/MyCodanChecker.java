package ch.hsr.ifs.cdttesting.example.examplecodantest;

import ch.hsr.ifs.iltis.cpp.wrappers.AbstractIndexAstChecker;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import ch.hsr.ifs.iltis.cpp.ast.checker.helper.IProblemId;


/**
 * Note that this class should actually not be contained in the
 * testing-plugin-project but rather in the plugin-project itself (should be
 * self-explaining)
 */
public class MyCodanChecker extends AbstractIndexAstChecker {

   /**
    * Note that this is/must be the same problem-id as defined in plugin.xml
    */
   public enum MyProblemId implements IProblemId {
      EXAMPLE_ID("ch.hsr.ifs.myCodanProblemId");

      String id;

      MyProblemId(String id) {
         this.id = id;
      }

      @Override
      public String getId() {
         return id;
      }

   }

   /**
    * Note that this is/must be the same checker-id as defined in plugin.xml
    */
   public static final String MY_CHECKER_ID = "ch.hsr.ifs.myCodanCheckerId";

   @Override
   public void processAst(IASTTranslationUnit ast) {
      String name = "unknown";
      IASTDeclaration firstDecl = ast.getDeclarations()[0];
      if (firstDecl instanceof IASTFunctionDefinition) {
         IASTFunctionDefinition functionDecl = (IASTFunctionDefinition) firstDecl;
         name = functionDecl.getDeclarator().getName().getRawSignature();
      }
      reportProblem(MyProblemId.EXAMPLE_ID.getId(), firstDecl, name); // note that the name-string is inserted into the
      // "messagePattern" by replacing the "{0}" of the pattern.
   }
}
