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
import ch.hsr.ifs.iltis.cpp.ast.visitor.CallbackVisitor;


class ASTNodeCollector{

   private Thread thread;

   private BlockingQueue<IASTNode> pipe = new LinkedBlockingQueue<>();

   private CallbackVisitor visitor = new CallbackVisitor(this::handleNode);

   private List<CommentRelation> commentRelations;

   private boolean abortASAP = false;

   private boolean compareComments;

   ASTNodeCollector(IASTTranslationUnit ast, EnumSet<ComparisonArg> args) {
      this.compareComments = args.contains(ComparisonArg.COMPARE_COMMENTS);

      if (compareComments) {
         commentRelations = Arrays.stream(ast.getComments()).map(CommentRelation::new).collect(Collectors.toList());
      }

      thread = new Thread(() -> {
         ast.accept(visitor);
      });

   }

   public void start() {
      thread.start();
   }

   public void join() throws InterruptedException {
      thread.join();
   }
   
   public boolean isAlive() {
      return thread.isAlive();
   }
   
   public boolean hasNode() {
      return !pipe.isEmpty();
   }

   public IASTNode poll(int timeout, TimeUnit unit) throws InterruptedException {
      return pipe.poll(timeout, unit);
   }

   public void abort() {
      this.abortASAP = true;
   }
   
   public List<CommentRelation> getCommentRelations() {
      return commentRelations;
   }

   private int handleNode(IASTNode node) {
      try {
         updateAllCommentRelations(commentRelations, node);
         pipe.put(node);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      return abortASAP ? ASTVisitor.PROCESS_ABORT : ASTVisitor.PROCESS_CONTINUE;
   }

   private void updateAllCommentRelations(List<CommentRelation> relations, IASTNode node) {
      if (compareComments) {
         relations.forEach((relation) -> relation.update(node));
      }
   }

}
