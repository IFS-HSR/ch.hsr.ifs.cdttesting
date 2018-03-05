package ch.hsr.ifs.cdttesting.cdttest.comparison;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;
import ch.hsr.ifs.iltis.core.functional.Functional;
import ch.hsr.ifs.iltis.cpp.ast.visitor.CallbackVisitor;


public class NodeComparisonVisitor {

   private Thread leftThread;
   private Thread rightThread;

   private CyclicBarrier providedNodeBarrier = new CyclicBarrier(2);
   private CyclicBarrier handledNodeBarrier  = new CyclicBarrier(2);

   private IASTNode currentLeftNode;
   private IASTNode currentRightNode;

   private ComparisonResult lastResult = null;

   private IASTTranslationUnit leftTu;
   private IASTTranslationUnit rightTu;

   private CallbackVisitor leftVisitor  = new CallbackVisitor(this::handleNode);
   private CallbackVisitor rightVisitor = new CallbackVisitor(this::handleNode);

   private List<CommentRelation> leftCommentRelations;
   private List<CommentRelation> rightCommentRelations;

   private EnumSet<ComparisonArg> args;
   private boolean                compareComments;

   public NodeComparisonVisitor(IASTTranslationUnit left, IASTTranslationUnit right, EnumSet<ComparisonArg> args) {
      this.leftTu = left;
      this.rightTu = right;
      this.args = args;
      this.compareComments = args.contains(ComparisonArg.COMPARE_COMMENTS);

      if (compareComments) {
         leftCommentRelations = Arrays.stream(left.getComments()).map(CommentRelation::new).collect(Collectors.toList());
         rightCommentRelations = Arrays.stream(right.getComments()).map(CommentRelation::new).collect(Collectors.toList());
      }

      leftThread = new Thread(() -> {
         leftTu.accept(leftVisitor);
      });

      rightThread = new Thread(() -> {
         rightTu.accept(rightVisitor);
      });
   }

   public ComparisonResult compare() {
      leftThread.start();
      rightThread.start();

      try {
         leftThread.join();
         rightThread.join();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      if (lastResult.isNotEqual()) {
         return lastResult;
      } else if (compareComments) {
         return commentsEqual();
      } else {
         return new ComparisonResult(ComparisonState.EQUAL);
      }
   }

   public ComparisonResult commentsEqual() {
      return Functional.zip(leftCommentRelations, rightCommentRelations).map((pair) -> CommentRelation.equals(pair.first(), pair.second())).filter(
            ComparisonResult::isNotEqual).findFirst().orElse(new ComparisonResult(ComparisonState.EQUAL));
   }

   protected int handleNode(IASTNode node) {
      if (Thread.currentThread() == leftThread) {
         updateAllCommentRelations(leftCommentRelations, node);
         currentLeftNode = node;
         try {
            providedNodeBarrier.await();
            lastResult = ASTComparison.equals(currentLeftNode, currentRightNode, args);
            handledNodeBarrier.await();
         } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
         }

      } else if (Thread.currentThread() == rightThread) {
         currentRightNode = node;
         updateAllCommentRelations(rightCommentRelations, node);
         try {
            providedNodeBarrier.await();
            handledNodeBarrier.await();
         } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
         }
      }
      return lastResult.isEqual() ? ASTVisitor.PROCESS_CONTINUE : ASTVisitor.PROCESS_ABORT;
   }

   private void updateAllCommentRelations(List<CommentRelation> relations, IASTNode node) {
      if (compareComments) {
         relations.forEach((relation) -> relation.update(node));
      }
   }

}
