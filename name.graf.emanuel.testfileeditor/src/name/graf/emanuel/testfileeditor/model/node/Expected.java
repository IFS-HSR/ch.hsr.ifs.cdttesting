package name.graf.emanuel.testfileeditor.model.node;

import org.eclipse.jface.text.Position;


public class Expected implements Node {

   private Test     parent;
   private String   text;
   private Position pos;

   public Expected(final Test parent, final String text, final Position pos) {
      super();
      this.parent = parent;
      this.text = text;
      this.pos = pos;
   }

   @Override
   public Node[] getChildren() {
      return null;
   }

   @Override
   public Node getParent() {
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
   public String toString() {
      return this.text;
   }

   @Override
   public boolean equals(final Object obj) {
      return this.hashCode() == obj.hashCode();
   }
}
