package ch.hsr.ifs.pasta.tree;

/**
 * The layout algorithm.
 *
 * @author silflow
 *
 */
class JBaum {

   private static float INITIAL_DISTANCE = 1f;

   private static <T> Node<T> firstWalk(final Node<T> node, final float siblingDistance, final float branchDistance) {

      if (node.children().isEmpty()) {
         if (node.hasLeftSibling()) {
            node.setX(node.leftSibling().x() + node.leftSibling().width() + siblingDistance);
         } else {
            node.setX(0.0f);
         }
      } else {
         Node<T> defaultAncestor = node.children().get(0);
         for (final Node<T> child : node.children()) {
            firstWalk(child, siblingDistance, branchDistance);
            defaultAncestor = apportion(child, defaultAncestor, branchDistance);
         }
         executeShifts(node);
         final float midPoint = (node.leftMostChild().x() + node.rightMostChild().x() + node.rightMostChild().width()) / 2f;
         if (node.hasLeftSibling()) {
            node.setX(node.leftSibling().x() + node.leftSibling().width() + siblingDistance);
            node.setMod(node.x() + node.width() / 2 - midPoint);
         } else {
            node.setX(midPoint - (node.width()) / 2);
         }
      }
      return node;
   }

   private static <T> Node<T> apportion(final Node<T> node, final Node<T> defaultAncestor, final float branchDistance) {
      if (node.hasLeftSibling()) {
         Node<T> innerRight = node;
         Node<T> outerRight = node;
         Node<T> innerLeft = node.leftSibling();
         Node<T> outerLeft = node.leftMostSibling();

         float sInnerRight = node.mod();
         float sOuterRight = node.mod();
         float sInnerLeft = innerLeft.mod();
         float sOuterLeft = outerLeft.mod();

         while (innerLeft.rightMostChild() != null && innerRight.leftMostChild() != null) {
            innerLeft = innerLeft.rightMostChild();
            innerRight = innerRight.leftMostChild();
            outerLeft = outerLeft.leftMostChild();
            outerRight = outerRight.rightMostChild();
            outerRight.setAncestor(node);

            final float shift = (innerLeft.x() + innerLeft.width() + sInnerLeft) - (innerRight.x() + sInnerRight) + branchDistance;
            if (shift > 0) {
               moveSubtree(ancestor(innerLeft, node, defaultAncestor), node, shift);
               sInnerRight += shift;
               sOuterRight += shift;
            }

            sInnerLeft += innerLeft.mod();
            sInnerRight += innerRight.mod();
            sOuterLeft += outerLeft.mod();
            sOuterRight += outerRight.mod();
         }

         if (innerLeft.rightMostChild() != null && outerRight.rightMostChild() == null) {
            outerRight.setThread(innerLeft.rightMostChild());
            outerRight.setMod(outerRight.mod() + sInnerLeft - sOuterRight);
         } else {
            if (innerRight.leftMostChild() != null && outerLeft.leftMostChild() == null) {
               outerLeft.setThread(innerRight.leftMostChild());
               outerLeft.setMod(outerLeft.mod() + sInnerRight - sOuterLeft);
            }
            return node;
         }
      }
      return defaultAncestor;
   }

   private static <T> Node<T> ancestor(final Node<T> innerLeft, final Node<T> node, final Node<T> defaultAncestor) {
      return (node.parent().children().contains(innerLeft.ancestor())) ? innerLeft.ancestor() : defaultAncestor;
   }

   private static <T> void moveSubtree(final Node<T> wl_anc, final Node<T> wr_node, final float shift) {
      final float subtrees = wr_node.number() - wl_anc.number();
      wr_node.setChange(wr_node.change() - (shift / subtrees));
      wr_node.setShift(wr_node.shift() + shift);
      wl_anc.setChange(wl_anc.change() + (shift / subtrees));
      wr_node.setX(wr_node.x() + shift);
      wr_node.setMod(wr_node.mod() + shift);
   }

   private static <T> void executeShifts(final Node<T> node) {
      float shift = 0;
      float change = 0;
      for (int i = node.children().size() - 1; i > -1; --i) {
         final Node<T> child = node.children().get(i);
         child.setX(child.x() + shift);
         child.setMod(child.mod() + shift);
         change += child.change();
         shift += child.shift() + change;
      }
   }

   private static <T> float secondWalk(final Node<T> node, final float m, final float depth, Float min) {
      node.setX(node.x() + m);
      node.setY(depth);
      if (min == null || node.x() < min) {
         min = node.x();
      }
      for (final Node<T> child : node.children()) {
         min = secondWalk(child, m + node.mod(), depth + JBaum.INITIAL_DISTANCE, min);
      }
      return min;
   }

   private static <T> void thirdWalk(final Node<T> node, final float n) {
      node.setX(node.x() + n);
      for (final Node<T> child : node.children()) {
         thirdWalk(child, n);
      }
   }

   public static <T> Node<T> adjustTree(final Node<T> tree, final float siblingDistance, final float branchDistance) {
      final Node<T> intermediate = firstWalk(tree, siblingDistance, branchDistance);
      final float min = secondWalk(intermediate, 0f, 0f, null);
      if (min < 0) {
         thirdWalk(intermediate, -min);
      }
      return intermediate;
   }

   protected static <T> void reset(final Node<T> node) {

      node.visit(n -> {
         n.setShift(0);
         n.setMod(0);
         n.setX(0);
         n.setChange(0);
         n.setThread(null);
         n.setAncestor(n);
         return NodeVisitor.AfterVisitBehaviour.Continue;
      });
   }
}
