package ch.hsr.ifs.cdttesting.cdttest.comparison;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;
import ch.hsr.ifs.iltis.core.functional.Functional;


public class NodeComparisonVisitor {

   private ASTNodeCollector leftCollector;
   private ASTNodeCollector rightCollector;

   private ComparisonResult lastFailingResult = null;

   private EnumSet<ComparisonArg> args;
   private boolean                compareComments;

   public NodeComparisonVisitor(IASTTranslationUnit left, IASTTranslationUnit right, EnumSet<ComparisonArg> args) {
      this.leftCollector = new ASTNodeCollector(left, args);
      this.rightCollector = new ASTNodeCollector(right, args);
      this.args = args;
      this.compareComments = args.contains(ComparisonArg.COMPARE_COMMENTS);
   }

   public ComparisonResult compare() {
      leftCollector.start();
      rightCollector.start();

      try {
         compareNodes();
         leftCollector.join();
         rightCollector.join();

      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      if (lastFailingResult != null) {
         return lastFailingResult;
      } else if (compareComments) {
         return commentsEqual();
      } else {
         return new ComparisonResult(ComparisonState.EQUAL);
      }
   }

   public ComparisonResult commentsEqual() {
      return Functional.zip(leftCollector.getCommentRelations(), rightCollector.getCommentRelations()).map((pair) -> CommentRelation.equals(pair
            .first(), pair.second(), args)).filter(ComparisonResult::isNotEqual).findFirst().orElse(new ComparisonResult(ComparisonState.EQUAL));
   }

   protected void compareNodes() throws InterruptedException {
      while (leftCollector.isAlive() || rightCollector.isAlive() || leftCollector.hasNode() || rightCollector.hasNode()) {
         IASTNode expected = null;
         do {
            expected = leftCollector.poll(10, TimeUnit.MILLISECONDS);
         } while (expected == null && leftCollector.isAlive());

         IASTNode actual = null;
         do {
            actual = rightCollector.poll(10, TimeUnit.MILLISECONDS);
         } while (actual == null && rightCollector.isAlive());

         if (expected == null && actual == null) break;

         ComparisonResult tempResult = ASTComparison.equals(expected, actual, args);
         if (tempResult.isNotEqual()) {
            lastFailingResult = tempResult;
            leftCollector.abort();
            rightCollector.abort();
         }
      }
   }
}
