package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;


public class Selection implements Node {

   private Position pos;
   private File     parent;

   public Selection(final Position pos, final File parent) {
      super();
      this.pos = pos;
      this.parent = parent;
   }

   @Override
   public Node[] getChildren() {
      return null;
   }

   @Override
   public Object getParent() {
      return this.parent;
   }

   @Override
   public Position getPosition() {
      return this.pos;
   }

   @Override
   public boolean hasChildren() {
      return false;
   }

   @Override
   public boolean equals(final Object obj) {
      return this.hashCode() == obj.hashCode();
   }

   @Override
   public int hashCode() {
      return super.hashCode();
   }

   @Override
   public String toString() {
      return "";
   }
}
