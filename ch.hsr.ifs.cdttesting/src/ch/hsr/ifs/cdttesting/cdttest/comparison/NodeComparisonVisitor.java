package ch.hsr.ifs.cdttesting.cdttest.comparison;

import java.util.EnumSet;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import ch.hsr.ifs.iltis.core.data.StringPrintStream;
import ch.hsr.ifs.iltis.core.functional.Functional;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonAttrID;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonAttribute;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;


public class NodeComparisonVisitor {

   private ASTNodeCollector leftCollector;
   private ASTNodeCollector rightCollector;

   private EnumSet<ComparisonArg> args;
   private boolean                compareComments;

   public NodeComparisonVisitor(IASTTranslationUnit leftAST, IASTTranslationUnit rightAST, EnumSet<ComparisonArg> args) {
      this.leftCollector = new ASTNodeCollector(leftAST, args);
      this.rightCollector = new ASTNodeCollector(rightAST, args);
      this.args = args;
      this.compareComments = args.contains(ComparisonArg.COMPARE_COMMENTS);
   }

   public ComparisonResult compare() {
      leftCollector.schedule();
      rightCollector.schedule();
      ComparisonResult result;
      try {
         result = compareNodes();
         leftCollector.join();
         rightCollector.join();
         if (result.isUnequal()) {
            return result;
         } else if (compareComments) {
            return commentsEqual();
         } else {
            return new ComparisonResult(ComparisonState.EQUAL);
         }
      } catch (InterruptedException e) {
         StringPrintStream stream = StringPrintStream.createNew();
         e.printStackTrace();
         e.printStackTrace(stream);
         return new ComparisonResult(ComparisonState.INTERRUPTED, new ComparisonAttribute(ComparisonAttrID.ACTUAL, e.getClass().getSimpleName(),
               stream.toString()));
      }
   }

   public ComparisonResult commentsEqual() {
      return Functional.zip(leftCollector.getCommentRelations(), rightCollector.getCommentRelations()).map((pair) -> CommentRelation.equals(pair
            .first(), pair.second(), args)).filter(ComparisonResult::isUnequal).findFirst().orElse(new ComparisonResult(ComparisonState.EQUAL));
   }

   protected ComparisonResult compareNodes() throws InterruptedException {
      while (true) {

         IASTNode expected = leftCollector.poll();
         IASTNode actual = rightCollector.poll();

         if (expected instanceof EOTNode && actual instanceof EOTNode) break;

         ComparisonResult result = ASTComparison.equals(expected, actual, args);
         if (result.isUnequal()) {
            leftCollector.abort();
            rightCollector.abort();
            return result;
         }
      }
      return new ComparisonResult(ComparisonState.EQUAL);
   }
}
