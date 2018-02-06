package ch.hsr.ifs.pasta.tree;

import static org.junit.Assert.assertEquals;

import java.awt.geom.Point2D;

import org.junit.Test;


public class JBaumTest {

   @Test
   public void testShouldCenterRootNodeOnXCoordinateInSimpleBinaryTree() {
      Node<String> root = createBinayTree();
      root.adjust();
      assertEquals(1d, root.x(), 0.0d);
   }

   @Test
   public void testShouldCenterPlaceChildrenOneLevelOfDepthDeeperThanRoot() {
      Node<String> root = createBinayTree();
      root.adjust();
      assertEquals(2, root.children().size());
      for (Node<String> child : root.children()) {
         assertEquals(1.0d, child.y(), 0.0d);
      }
   }

   @Test
   public void testShouldPositionEachNodeCorrectly() {
      Node<Point2D> root = createComplexTree();
      root.adjust();
      int checks = assertTreePositions(root);
      assertEquals(9, checks);
   }

   @Test
   public void testShouldNotCrashWhenAdjustingTree() {
      Node<Point2D> root = createBuggedTree();
      root.adjust();
      assertTreePositions(root);
   }

   private int assertTreePositions(Node<Point2D> root) {
      Point2D expectedPosition = root.data();
      assertEquals(expectedPosition.getX(), root.x(), 0.0d);
      assertEquals(expectedPosition.getY(), root.y(), 0.0d);

      int checks = 0;
      for (Node<Point2D> child : root.children()) {
         checks += assertTreePositions(child);
      }
      return checks + 1;
   }

   private Node<String> createBinayTree() {
      Node<String> root = new Node<>("root");
      root.addChild(new Node<String>("leftChild"));
      root.addChild(new Node<String>("rightChild"));
      root.adjust();
      return root;
   }

   private Node<Point2D> createComplexTree() {
      Node<Point2D> root = new Node<Point2D>(new Point2D.Double(6.0, 0.0));
      Node<Point2D> leftChild = new Node<Point2D>(new Point2D.Double(2.0, 1.0));
      Node<Point2D> leftChildChild = new Node<Point2D>(new Point2D.Double(0.0, 2.0));
      Node<Point2D> leftChildChild2 = new Node<Point2D>(new Point2D.Double(2.0, 2.0));
      Node<Point2D> leftChildChild3 = new Node<Point2D>(new Point2D.Double(4.0, 2.0));
      Node<Point2D> centerChild = new Node<Point2D>(new Point2D.Double(6.0, 1.0));
      Node<Point2D> centerChildChild = new Node<Point2D>(new Point2D.Double(6.0, 2.0));
      Node<Point2D> centerRightChild = new Node<Point2D>(new Point2D.Double(8.0, 1.0));
      Node<Point2D> rightChild = new Node<Point2D>(new Point2D.Double(10.0, 1.0));

      root.addChild(leftChild);
      root.addChild(centerChild);
      root.addChild(centerRightChild);
      root.addChild(rightChild);
      leftChild.addChild(leftChildChild);
      leftChild.addChild(leftChildChild2);
      leftChild.addChild(leftChildChild3);
      centerChild.addChild(centerChildChild);

      return root;
   }

   private static Node<Point2D> createBuggedTree() {
      Node<Point2D> root = new Node<Point2D>(new Point2D.Double(5, 0));
      Node<Point2D> leftChild = new Node<Point2D>(new Point2D.Double(2, 1));
      Node<Point2D> leftChildChild = new Node<Point2D>(new Point2D.Double(0, 2));
      Node<Point2D> leftChildChild2 = new Node<Point2D>(new Point2D.Double(2, 2));
      Node<Point2D> leftChildChild3 = new Node<Point2D>(new Point2D.Double(4, 2));
      Node<Point2D> rightChild = new Node<Point2D>(new Point2D.Double(8, 1));
      Node<Point2D> rightChildChild = new Node<Point2D>(new Point2D.Double(6, 2));
      Node<Point2D> rightChildChild2 = new Node<Point2D>(new Point2D.Double(8, 2));
      Node<Point2D> rightChildChild3 = new Node<Point2D>(new Point2D.Double(10, 2));
      root.addChild(leftChild);
      root.addChild(rightChild);
      leftChild.addChild(leftChildChild);
      leftChild.addChild(leftChildChild2);
      leftChild.addChild(leftChildChild3);
      rightChild.addChild(rightChildChild);
      rightChild.addChild(rightChildChild2);
      rightChild.addChild(rightChildChild3);

      leftChildChild2.addChild(new Node<Point2D>(new Point2D.Double(2, 3)));
      rightChildChild2.addChild(new Node<Point2D>(new Point2D.Double(8, 3)));
      return root;

   }
}
