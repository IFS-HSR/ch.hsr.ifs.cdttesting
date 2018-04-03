package name.graf.emanuel.testfileeditor.ui;

import java.util.Vector;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;


public class OutlineTreeViewer extends TreeViewer {

   private Vector<Integer> expanded;

   public OutlineTreeViewer(final Composite parent) {
      super(parent);
      this.expanded = new Vector<Integer>();
   }

   public OutlineTreeViewer(final Composite parent, final int style) {
      super(parent, style);
      this.expanded = new Vector<Integer>();
   }

   public OutlineTreeViewer(final Tree tree) {
      super(tree);
      this.expanded = new Vector<Integer>();
   }

   public void saveExpandedState() {
      final Object[] exo = this.getExpandedElements();
      this.expanded.clear();
      Object[] array;
      for (int length = (array = exo).length, i = 0; i < length; ++i) {
         final Object object = array[i];
         this.expanded.add(object.hashCode());
      }
   }

   public void loadExpandedState() {
      this.expandToLevel(2);
      final Object[] exp = this.getExpandedElements();
      Object[] array;
      for (int length = (array = exp).length, i = 0; i < length; ++i) {
         final Object object = array[i];
         if (this.isExpandable(object) && this.expanded.contains(object.hashCode())) {
            this.setExpandedState(object, true);
            this.expandPath(object);
         } else {
            this.setExpandedState(object, false);
         }
      }
   }

   private void expandPath(final Object root) {
      final Object[] childs = this.getRawChildren(root);
      Object[] array;
      for (int length = (array = childs).length, i = 0; i < length; ++i) {
         final Object object = array[i];
         if (this.isExpandable(object) && this.expanded.contains(object.hashCode())) {
            this.setExpandedState(object, true);
            this.expandPath(object);
         } else {
            this.setExpandedState(object, false);
         }
      }
   }

   @Override
   public int getAutoExpandLevel() {
      return 2;
   }
}
