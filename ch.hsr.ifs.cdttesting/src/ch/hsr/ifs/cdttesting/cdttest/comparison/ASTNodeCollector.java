package ch.hsr.ifs.cdttesting.cdttest.comparison;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import ch.hsr.ifs.iltis.cpp.ast.visitor.CallbackVisitor;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;


class ASTNodeCollector {

   private Job job;

   private BlockingQueue<IASTNode> pipe = new LinkedBlockingQueue<>();

   private CallbackVisitor visitor = new CallbackVisitor(this::handleNode);

   private List<CommentRelation> commentRelations;

   private boolean abortASAP = false;

   private boolean compareComments;

   private boolean visitingFinished = false;

   private EOTNode eotNode;

   ASTNodeCollector(IASTTranslationUnit ast, EnumSet<ComparisonArg> args) {
      this.compareComments = args.contains(ComparisonArg.COMPARE_COMMENTS);
      this.eotNode = new EOTNode(ast);

      if (compareComments) {
         commentRelations = Stream.of(ast.getComments()).map(CommentRelation::new).collect(Collectors.toList());
      }

      IPath filePath = new Path(ast.getContainingFilename());

      job = Job.create("Node collector on " + filePath.lastSegment(), mon -> {
         ast.accept(visitor);
         visitingFinished = true;
      });

      job.addJobChangeListener(new JobChangeAdapter() {

         @Override
         public void done(IJobChangeEvent event) {
            visitingFinished = true;
            super.done(event);
         }

      });

   }

   public void schedule() {
      job.schedule();
   }

   public void join() throws InterruptedException {
      job.join();
   }

   public IASTNode poll() throws InterruptedException {
      IASTNode node;
      do {
         if (visitingFinished && pipe.isEmpty()) {
            node = eotNode;
         } else {
            node = pipe.poll();
         }
      } while (node == null);
      return node;
   }

   public void abort() {
      this.abortASAP = true;
   }

   public List<CommentRelation> getCommentRelations() {
      return commentRelations;
   }

   private int handleNode(IASTNode node) {
      if (!node.isPartOfTranslationUnitFile()) return ASTVisitor.PROCESS_SKIP;
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
