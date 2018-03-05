package ch.hsr.ifs.cdttesting.cdttest.comparison;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonArg;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonResult;
import ch.hsr.ifs.cdttesting.cdttest.comparison.ASTComparison.ComparisonState;


public class CommentRelation {

   public final IASTComment       comment;
   private final IASTFileLocation commentLoc;
   public IASTNode                previous;
   public IASTNode                next;
   public IASTNode                enclosing;

   public CommentRelation(IASTComment comment) {
      this.comment = comment;
      this.commentLoc = comment.getFileLocation();
   }

   public CommentRelation(IASTComment comment, IASTNode previous, IASTNode next) {
      this(comment);
      this.previous = previous;
      this.next = next;
   }

   public static ComparisonResult equals(CommentRelation left, CommentRelation right) {
      if (right == null || left == null) { return new ComparisonResult(ComparisonState.NO_COUNTERPART, ASTComparison
            .generateBasicComparisonResultAttributes(left == null ? null : left.comment, right == null ? null : right.comment)); }
      ComparisonResult commentResult = ASTComparison.equals(left.comment, right.comment, ComparisonArg.emptySet());
      ComparisonResult previousResult = ASTComparison.equals(left.previous, right.previous, ComparisonArg.emptySet());
      ComparisonResult nextResult = ASTComparison.equals(left.next, right.next, ComparisonArg.emptySet());
      ComparisonResult enclosingResult = ASTComparison.equals(left.enclosing, right.enclosing, ComparisonArg.emptySet());
      if (commentResult.isNotEqual() || previousResult.isNotEqual() || nextResult.isNotEqual() || enclosingResult.isNotEqual()) {
         return new ComparisonResult(ComparisonState.DIFFERENT_COMMENT, commentResult.attributes);
      } else {
         return new ComparisonResult(ComparisonState.EQUAL);
      }
   }

   public CommentRelation update(IASTNode node) {
      IASTFileLocation loc = node.getFileLocation();
      if (loc == null) return this;

      if (nodeEnclosesComment(loc)) {
         if (enclosing == null || enclosesTighter(loc)) {
            enclosing = node;
         }
      } else if (nodeIsLeadingToComment(loc)) {
         if (previous == null || getEndOffset(loc) > getEndOffset(previous.getFileLocation())) {
            previous = node;
         }
      } else if (nodeIsTrailingToComment(loc)) {
         if (next == null || getStartOffset(loc) < getStartOffset(next.getFileLocation())) {
            next = node;
         }
      }

      return this;
   }

   private boolean nodeIsLeadingToComment(IASTFileLocation loc) {
      return getEndOffset(loc) <= getStartOffset(commentLoc);
   }

   private boolean nodeIsTrailingToComment(IASTFileLocation loc) {
      return getStartOffset(loc) >= getEndOffset(commentLoc);
   }

   private boolean enclosesTighter(IASTFileLocation loc) {
      return startDistanceToComment(loc) + endDistanceToComment(loc) < startDistanceToComment(enclosing.getFileLocation()) + endDistanceToComment(
            enclosing.getFileLocation());
   }

   private boolean nodeEnclosesComment(IASTFileLocation loc) {
      return nodeStartsBeforeComment(loc) && nodeEndsAfterComment(loc);
   }

   private boolean nodeEndsAfterComment(IASTFileLocation loc) {
      return getEndOffset(loc) > getEndOffset(commentLoc);
   }

   private int getEndOffset(IASTFileLocation loc) {
      return getStartOffset(loc) + loc.getNodeLength();
   }

   private boolean nodeStartsBeforeComment(IASTFileLocation loc) {
      return getStartOffset(loc) < getStartOffset(commentLoc);
   }

   private int getStartOffset(IASTFileLocation loc) {
      return loc.getNodeOffset();
   }

   private int startDistanceToComment(IASTFileLocation loc) {
      return getStartOffset(commentLoc) - getStartOffset(loc);
   }

   private int endDistanceToComment(IASTFileLocation loc) {
      return getEndOffset(commentLoc) - getEndOffset(loc);
   }

   public boolean isInsideANode() {
      return previous == next;
   }
}
