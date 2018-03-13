package ch.hsr.ifs.cdttesting.cdttest.comparison;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

   private BlockingQueue<IASTNode> leftPipe  = new LinkedBlockingQueue<>();
   private BlockingQueue<IASTNode> rightPipe = new LinkedBlockingQueue<>();

   private ComparisonResult lastFailingResult = null;

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
         compareNodes();
         leftThread.join();
         rightThread.join();

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
      return Functional.zip(leftCommentRelations, rightCommentRelations).map((pair) -> CommentRelation.equals(pair.first(), pair.second(), args))
            .filter(ComparisonResult::isNotEqual).findFirst().orElse(new ComparisonResult(ComparisonState.EQUAL));
   }

   protected void compareNodes() throws InterruptedException {
      while (leftThread.isAlive() || rightThread.isAlive() || !leftPipe.isEmpty() || !rightPipe.isEmpty()) {
         IASTNode expected = null;
         do {
            expected = leftPipe.poll(10, TimeUnit.MILLISECONDS);
         } while (expected == null && leftThread.isAlive());

         IASTNode actual = null;
         do {
            actual = rightPipe.poll(10, TimeUnit.MILLISECONDS);
         } while (actual == null && rightThread.isAlive());

         if (expected == null && actual == null) break;

         ComparisonResult tempResult = ASTComparison.equals(expected, actual, args);
         if (tempResult.isNotEqual()) lastFailingResult = tempResult;
      }
   }

   protected int handleNode(IASTNode node) {
      if (Thread.currentThread() == leftThread) {
         try {
            updateAllCommentRelations(leftCommentRelations, node);
            leftPipe.put(node);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      } else if (Thread.currentThread() == rightThread) {
         try {
            updateAllCommentRelations(rightCommentRelations, node);
            rightPipe.put(node);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      return lastFailingResult == null ? ASTVisitor.PROCESS_CONTINUE : ASTVisitor.PROCESS_ABORT;
   }

   private void updateAllCommentRelations(List<CommentRelation> relations, IASTNode node) {
      if (compareComments) {
         relations.forEach((relation) -> relation.update(node));
      }
   }

}
