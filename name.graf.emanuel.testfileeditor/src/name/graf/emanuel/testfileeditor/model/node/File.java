package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;


public class File implements Node {

   private final String   name;
   private final Position pos;
   private final Position head;
   private final Test     parent;
   private Expected       exp;
   private Selection      sel;

   public File(final String name, final Position pos, final Position head, final Test parent) {
      super();
      exp = null;
      sel = null;
      this.name = name;
      this.pos = pos;
      this.head = head;
      this.parent = parent;
   }

   @Override
   public Node[] getChildren() {
      final int i = howManyChildren();
      if (i > 0) {
         int index = 0;
         final Node[] children = new Node[i];
         if (sel != null) {
            children[index++] = sel;
         }
         if (exp != null) {
            children[index++] = exp;
         }
         return children;
      }
      return null;
   }

   @Override
   public Object getParent() {
      return parent;
   }

   @Override
   public Position getPosition() {
      return pos;
   }

   public Position getHeadPosition() {
      return head;
   }

   public boolean containsOffset(final int offset) {
      return pos.overlapsWith(offset, 1);
   }

   @Override
   public boolean hasChildren() {
      return howManyChildren() > 0;
   }

   @Override
   public String toString() {
      return name;
   }

   @Override
   public int hashCode() {
      final long namenHash = name.hashCode();
      return (int) namenHash + pos.offset;
   }

   @Override
   public boolean equals(final Object obj) {
      return hashCode() == obj.hashCode();
   }

   public void setExpected(final Expected node) {
      exp = node;
   }

   public void setSelection(final Selection node) {
      sel = node;
   }

   private int howManyChildren() {
      int length = 0;
      if (sel != null) {
         ++length;
      }
      if (exp != null) {
         ++length;
      }
      return length;
   }
}
